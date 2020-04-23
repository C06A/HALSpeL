# HALSpeL

This is a Kotlin DSL wrapper for [Fuel library](https://github.com/kittinunf/fuel) to work with HAL-API applications
as a collection of Resources.

This wrapper provides DSL allowing Client application to navigate HAL resources on the server using high level
abstract Resource objects.

This project contains follow modules:

1. [hal-spel](https://github.com/C06A/HALSpeL/tree/develop/hal-spel): contains the wrapper itself
1. fuel-spel: provides some helpful functions to configure the underlying Fuel library
1. [go-about](https://github.com/C06A/HALSpeL/tree/develop/go-about): is the sample application using HALSpeL
to access information about many geographical locations
1. [oxford](https://github.com/C06A/HALSpeL/tree/develop/oxford): is the sample application
to access information about classes in Oxford
1. [geoIP](https://github.com/C06A/HALSpeL/tree/develop/geoIP): is the sample application
to access information about IP-address from few different services supporting HAL

For more details check README document of each module.

Also `oxford` and `geoIP` modules include implementations of Hi-/Low-level API from Java and Groovy code.

In additon `oxford` module demonstrates an approach to document API in AsciiDoctor format including
real "live" data from running regression tests.
