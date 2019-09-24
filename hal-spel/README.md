# HALSpeL

This Kotlin DSL wrapper of Fuel library allowing to access HAL server as collection of Resources.
It requires minimal coupling between Client and Server.

The HAL standard is an extension of the REST API. This means the each URL endpoint points to the Resource,
which can be created, retrieved, updated and removed. In order to use regular REST API the Client should
somehow build URL for each endpoint. This strongly coupling Client and Server. 

In contrast to REST fully implemented HAL standard allows Client to do not "know" any server's URL endpoints,
but discover them from other Resources. The only endpoint Client should have is the main entry point -- base
URL to the server. That endpoint will return the first Resource, which contains References on other Resources for
Client to start with. This makes coupling between Client aтв Server weak.

## Include wrapper into Gradle build

The distribution of the wrapper consists of single library published in BinTray repository. In order to include
it into build file add:

```androiddatabinding
compile("hal.spel:hal-spel:0.1")
``` 

## Overview high level DSL

The most used object in HALSpeL is a Resource. It contains References on other Resources, embedded Resources and attributes.

In some rare cases Client may access Answer object which contains request, response and result objects provided by Fuel
(see examples below).

### Actions on Resource

The Client can call one or more follow chain methods on any Resource:

* `FETCH` -- to request existing Resource from the Server
* `CREATE` -- to pass to the Server data to create a new Resource
* `REPLACE` -- to pass to the Server data to completely replace existing Resource
* `UPDATE` -- to pass to the Server data to replace individual fields values in existing Resource
* `REMOVE` -- to delete existing Resource

All methods requires first parameter -- the relation (`rel`) of the Reference to use.

Follow parameters may be a `Map<String, Any?>` or a sequence of `Pair<String, Any?>` providing substitution values
for URI Template in case Reference is `templated`.

If Reference is not templated or need no values for any placeholders following parameters need to have labels.

For `CREATE`, `REPLACE` and `UPDATE` next parameter contains the data to pass in.
It could be of type `String` or (for all but `UPDATE`) `File`, `Collection<File>` or `Map<String, File>`.

The `toString()` method on Resource produces multiline representation of the resource listing relations in it groups 
as References (links), Resources (embedded) and Attributes. This relations can be used to access data in the Resource.

### embedded Resources

Each Resource referencing its embedded Resource with Relation (`rel`). In order to retrieve embedded Resource
the Client should use parenthesis. This operation won't need to send HTTP request as embedded Resource
was received with enclosing Resource. Usually embedded Resource contains abbreviated version of the Resource.

### attribute of the Resource

The attributes of the Resource contain actual payload. They can represent a JSON value like String, Number (Int, Float, etc.),
Boolean, value `null`, Maps and Collections. All of them are wrapped in KotON instant.
To access attribute of the Resource the Client should put relation into square brackets and then de-reference result
by using parenthesis. This allows to address directly the value embedded deep into JSON structure. If provided relation
doesn't exist in the JSON value the KotON value will wrap the value `null`.

The final de-referencing parenthesis may specify the type the value will be casted to. This allows the Client code to remain
type-safe. If type is not specified the value will be of type `Any?`

## Usage of high level DSL

The function `halSpeL(...)` expects the Server's entry point URL and optionally values for Content Type and Templated flag.

The returned value accepts the chain method `FETCH` or `CREATE`. Both are similar to corresponding methods of the Resource,
but do not expect the first parameter (relation).

For example:

```kotlin
        halSpeL("https://api.goabout.com").FETCH()
```

will request entry point Resource from [Go About](http://goabout.com). That Resource includes
the relation to "health" Resource. The relation itself (according to HAL) is IRI and can be used to retrieve documentation 
about referenced Resource. So to check the health status of the service the Client can do follow:

```kotlin
        halSpeL("https://api.goabout.com")
                .FETCH()
                .FETCH("http://rels.goabout.com/health")
                .apply {
                    println("Health is " + this["status"]<String>()?.toUpperCase())
                }
```

This snippet will request the entry point Resource and then fetch the Resource referenced by relation `http://rels.goabout.com/health`.
Follow code will apply closure to the health status Resource. The closure will print the sentence
with `String` value of attribute `status` converted to upper case.


## Overview middle level DSL

For more detailed control of communication with Server methods mentioned in [high level DSL](#Overview high level DSL)
section also accept additional parameters.

### HTTP Headers

Follow the parameters for templated link the optional parameter specifies the additional headers.

The Fuel labrary supports "base headers" which will be added to all request automatically. The can be used for example
with authentication token, CSRS and other Client identification purposes. In addition each request needs it's own
headers like Content Type, Accept, Length, etc. methods accespt thous headers after template placeholders values
(see example below).

Default headers are:

* "ContentType:application/json"
* "Accept:application/hal+json"

### Tail closure

The last parameter of all methods is a closure, which will be called on the Answer object containing Fuel communication data.

This closure can for example check headers (including cookies) returned by the Server or choose the way to process
returned data base on the status.

## Usage of middle leDSL

For example in order to debug the problem we need to print out the body of the response.

```kotlin
        halSpeL("https://api.goabout.com")
                .FETCH()
                .FETCH("http://rels.goabout.com/feedback") {
                    println("Body:\n${GsonBuilder().setPrettyPrinting().create().toJson(body)}")
                }
```

As a tail closure is called aganst the Answer object the property `body` will contain the String representation of the
body in the Server's response.


## Overview low level DSL

For most fine control of communication, methods mentioned in [high level DSL](#Overview high level DSL)
section also accept one more closure. It provides the AOP (Aspect Oriented Programming) approach.

The AOP parameter name is `aspect` and in most cases it should be labeled. It will be used with all communications
following in the chain until another AOP closure parameter provided. Default value simply executes its parameter and
returns the result.

This closure takes as parameter another closure, which actually communicates with server and returns an Answer instance.
The `aspect` closure can modify the Link obect, it called on before executing it's parameter and analyze and modify
Answer object before return it as result of the communication.

Sample code demonstrates how use AOP closure defined as a variable `aopClosure`:

```kotlin
        halSpeL("https://api.goabout.com")
                .FETCH(aspect = aopClosure)
                .apply {
                    FETCH("http://rels.goabout.com/feedback")
                    FETCH("http://rels.goabout.com/health")
                    FETCH("http://rels.goabout.com/geocoder", "query" to "all")
                }
```

## Usecases foк low level DSL

Low level DSL should be used when the request needs modification before it is sent to server or response needs adjustmens
before it can be used for follow requests.

It is also useful to define action which should be repeated with each request.

### Logging communicatons

Follow sample code defines `aopClosure` value to log Link's name or href before sending request and curl command, status and
body from response.

```kotlin
        val aopClosure: (Link.(Link.() -> Answer) -> Answer) = {
            if (name.isNullOrBlank()) {
                logger.debug("Link href: $href")
            } else {
                logger.debug("Link name: $name ($href)")
            }
            it().apply {
                logger.trace(request.cUrlString())
                logger.trace("Status: $status")
                logger.trace("Body:\n${GsonBuilder().setPrettyPrinting().create().toJson(body)}")
            }
        }
```

Using this definition with code above will result in logging info about each of requests.

### Authentication

Unauthenticated request for protected REST resource usually should return HTTP status 401 (UnAuthenticated).
It make sense if HAL response in this case includes link to submit credentials for authentication.

As any request may return this error (in case authentication  token expires) AOP closure can automatically re-authenticate
Client. For that `aopClosure` can be defined as

```kotlin
    val aopClosure: (Link.(Link.() -> Answer) -> Answer) = {
        it().run {
            var answer = this
            when (status) {
                HttpStatus.UNAUTHORIZED -> {
                    Resource(answer())
                            .CREATE("login", body = """{"username": "$un", "password": "$pw"}""") {
                                body?.let {
                                    val authentication = "${it["token_type"]} ${it["access_token"]}"
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

