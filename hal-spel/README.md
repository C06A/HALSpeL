# HALSpeL

This Kotlin DSL wrapper of Fuel library allowing to access HAL server as collection of Resources.
It requires minimal coupling between Client and Server.

[![license](https://img.shields.io/github/license/C06A/HALSpeL.svg)](https://github.com/C06A/HALSpeL/blob/master/LICENSE)
[![Download Latest](https://img.shields.io/badge/download-1.6.0-green.svg)](https://github.com/C06A/HALSpeL/releases/download/v1.6.0/hal-spel-1.6.0.jar)


The [HAL standard](https://en.wikipedia.org/wiki/Hypertext_Application_Language) is an extension of the REST API. This means that each URL endpoint points to the Resource,
which can be created, retrieved, updated and deleted. In order to use regular REST API the Client should
somehow build URL for each endpoint. This strongly couples Client and Server. 

In contrast to REST fully implemented HAL standard allows Client to do not "know" any server's URL endpoints,
but discover them from other Resources. The only endpoint Client should have is the main entry point -- base
URL to the server. That endpoint will return the first Resource, which contains References on other Resources for
Client to start with. This makes coupling between Client and Server weak.

## Include wrapper into Gradle build

The distribution of the wrapper consists of single library published in BinTray repository. In order to include
it into build file add:

```androiddatabinding
dependencies {
    compile("hal.spel:hal-spel:1.6.0")
}
``` 

## Overview high level DSL

The most used object in HALSpeL is a Resource. It contains embedded Resources, attributes and References to other Resources.

In some rare cases Client may access Answer object which contains request, response and result objects provided by Fuel
(see examples below).

### Actions on Resource

The Client can call one or chain of multiple follow methods on any Resource which supports them:

* `FETCH` -- to request existing Resource from the Server
* `CREATE` -- to pass to the Server data to create a new Resource
* `REPLACE` -- to pass to the Server data to completely replace existing Resource
* `UPDATE` -- to pass to the Server data to replace individual fields values in existing Resource
* `REMOVE` -- to delete existing Resource

All methods (except `FETCH` and `CREATE`) require first parameter -- the relation (`rel`) of the Reference to use.

Method `FETCH` without relation parameter (`rel`) would re-fetch current Resource using its `self` link.

Both `FETCH` and `CREATE` methods can be called on the object of type Link instead of Resource in which case they also
don't need relation parameter (`rel`).

Follow parameters may be a `Map<String, Any?>` or a sequence of `Pair<String, Any?>` providing substitution values
for [URI Template](https://tools.ietf.org/html/rfc6570) in case Reference or Link is `templated`.

Even if Reference is not templated or need no values for any placeholders following parameters need to be labeled.

For `CREATE`, `REPLACE` and `UPDATE` next parameter contains the data to pass in.
It could be of type `String` or (for all but `UPDATE`) `File`, `Collection<File>` or `Map<File, String>`. Later maps
each File to upload to the name of the corresponding part in multi-part body.

The `toString()` method on Resource produces multiline representation of the resource listing relations in it grouped 
as References (links), Resources (embedded) and Attributes. This relations can be used to access data in the Resource.

### embedded Resources

Each Resource referencing its embedded Resource with Relation (`rel`). In order to retrieve embedded Resource
use parenthesis enclosing relation with enclosing Resource object. This operation won't need to send HTTP request as embedded Resource
was received with enclosing Resource. Usually embedded Resource contains abbreviated version of the Resource and at least
`self` link to access the full version of the Resource.

In order to retrieve the complete Resource just call `FEATCH()` on embedded resource without relation parameter.

### attribute of the Resource

The attributes of the Resource contain actual payload. They can represent any JSON values like String, Number (Int, Float, etc.),
Boolean, value `null`, Maps and Collections. All of them are wrapped in [KotON](https://github.com/C06A/KotON) instant.
To access attribute of the Resource put relation into square brackets and then de-reference result by using empty parenthesis.
This allows to address directly the value embedded deep into JSON structure. If provided relation
doesn't exist in the JSON value the KotON value will wrap the value `null`.

The final de-referencing parenthesis may specify the type, the value will be casted to. This allows your code to remain
type-safe. If type is not specified the value will be of type `Any?`. If requested type doesn't match the type of the value
this call will throw cast exception.

## Usage of high level DSL

The function `halSpeL(...)` expects the Server's entry point URL and optionally values for Content Type and Templated flag.
It also accepts the `rel` value, which can be used for reporting.

The returned value accepts the chain methods `FETCH` or `CREATE`. Both are similar to corresponding methods of the Resource,
but do not expect the first parameter (relation).

For example:

```kotlin
        halSpeL("http://api.m.ox.ac.uk/").FETCH()
```

will request entry point Resource from [Mobile Oxford](http://m.ox.ac.uk). That Resource includes
the relation to "courses" Resource. The relation itself (according to HAL) is IRI and can be used to retrieve documentation 
about referenced Resource. So to check the list of courses application can do follow:

```kotlin
        halSpeL("http://api.m.ox.ac.uk/")
                .FETCH()
                .FETCH("app:courses")
```

This snippet will request the entry point Resource and then fetch the Resource referenced by relation `app:courses`.
This will return the list of courses to choose from.


## Overview of middle level DSL

For more detailed control of communication with Server methods mentioned in [high level DSL](#Overview high level DSL)
section also accept additional parameters.

### HTTP Headers

Follow the parameters for templated link the optional parameter specifies the additional headers.

The Fuel labrary supports "base headers" which will be added to all request automatically. That can be used for example
with authentication token, CSRS and other Client identification and security purposes. In addition each request needs it's own
headers like Content Type, Accept, etc. methods accepts thous headers after template placeholders values
(see example below).

Default headers are:

* "ContentType:application/json"
* "Accept:application/hal+json"

### Tail closure

The last parameter of all methods is a closure, which will be called on the Answer object (a Triplet returned by Fuel library).

This closure can for example check headers (including cookies) returned by the Server or choose the way to process
returned data base on the status.

## Usage of middle level DSL

For example in order to debug the problem developer can print out the body of the response.

```kotlin
        halSpeL("http://api.m.ox.ac.uk/")
                .FETCH()
                .FETCH("app:courses") {
                    println("Body:\n${ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body)}")
                }
```

As a tail closure is called against the Answer object the property `body` will contain the String representation of the
body in the Server's response.


## Aspect Oriented Programming

For most fine control of communication, methods mentioned in [high level DSL](#Overview high level DSL)
section also accept one more closure. This provides the AOP (Aspect Oriented Programming) approach.

The AOP parameter name is `aspect` and it should be labeled. It will be used with all communications
following in the chain until another AOP closure parameter provided. Default value simply executes its parameter and
returns the result.

This closure takes as parameter another closure, which actually communicates with server and returns an Answer instance.
The `aspect` closure can modify the Link obect, it called on before executing it's parameter and analyze and modify
Answer object before return it as result of the communication.

Sample code demonstrates how use AOP closure defined as a variable `aopClosure`:

```kotlin
        halSpeL("http://api.m.ox.ac.uk/")
                .FETCH(aspect = aopClosure)
                .apply {
                    FETCH("app:contacts")
                    FETCH("app:courses")
                    FETCH("app:events")
                }
```

### Logging Aspect Factory class

In order to simplify the logging of different parts of HTTP interactions with Server this wrapper provides a specially
designed class `LoggerFormatter`. Its constructor receives configuring parameters and method to build an aspect function.

Constructor expects the reporting function, list of elements from enum `ReportPart` and optional instance of another
`AspectFormatter` to chain them. In order to report at different level build the chian of `LoggerFormatter` with
corresponding reporting method of the logger and list of parts to report at this level. For example check the module
`go-about` in this repository. Also reporting function can be simple function printing String into STDOUT
or saving into the file.

### AsciiDoctor Tags Aspect Factory class

Another similar class `ADocTagFormatter` reporting the text for each part which is ready to be imported into AsciiDoctor document.

This class's constructor accepts as a first parameter a PrintWriter to print AsciiDoctor tag text to. This writer
could append text into the file or STDOUT for debugging purpose.

Later AsciiDoctor Gradle plugin can build the final documentation including chunks of real life communications into
the body of the documentation.

## Usecases for low level DSL

Low level DSL should be used when the request needs modification before it is sent to server or response needs adjustmens
before it can be used for follow requests.

It is also useful to define action which should be repeated with each request.

### Logging communicatons

Follow sample code defines `aopClosure` value to log Link's name or href before sending request and curl command, status and
body from response.

```kotlin
        val aopClosure: (Link.((Link.() -> Answer)?) -> Answer) = {
            if (name.isNullOrBlank()) {
                logger.debug("Link href: $href")
            } else {
                logger.debug("Link name: $name ($href)")
            }
            makeDefaultAspectIfNull(it)().apply {
                logger.trace(request.cUrlString())
                logger.trace("Status: $status")
                logger.trace("Body:\n${ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body)}")
            }
        }
```

Using this definition with code above will result in logging info about each of requests.

The method `makeDefaultAspectIfNull()` will return its parameter if it is not `null` and default aspect otherwise.

### Authentication

Unauthenticated request for protected REST resource usually should return HTTP status 401 (UnAuthenticated).
It make sense if HAL response in this case includes link to submit credentials for authentication.

As any request may return this error (in case authentication  token expires) AOP closure can automatically re-authenticate
Client. For that `aopClosure` can be defined as

```kotlin
    val aopClosure: (Link.((Link.() -> Answer)?) -> Answer) = {
        makeDefaultAspectIfNull(it)().run {
            var answer = this
            when (status) {
                HttpStatus.UNAUTHORIZED -> {
                    Resource(answer())
                            .CREATE("login", body = """{"username": "$un", "password": "$pw"}""") {
                                body?.let {
                                    val authentication = "${it["token_type"]<String>()} ${it["access_token"]<String>()}"
                                    FuelManager.instance.baseHeaders =
                                            (FuelManager.instance.baseHeaders ?: emptyMap()) +
                                                    ("Authorization" to authentication)
                                    answer = it()
                                }
                            }
                }
                else -> { }
            }
            answer
        }
    }
```

For more examples check files provided in the package `hal.spel.aspect`. They include some general usage AOP functiones
and classes.

## Overview of low level DSL

Similar to the [high level DSL](#Overview high level DSL) this library provides DSL for the strait REST API.
For example check module `geoIP`.

This DSL provides methods named after HTTP Methods: `GET`, `POST`, `PUT`, `PATCH` and `DELETE`. Each method can be called
on the Link object and returns Answer object.
