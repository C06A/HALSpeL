# HALSpeL

Kotlin DSL wrapper for Fuel to work with HAL-API applications as collection of Resources.

This wrapper provides DSL allowing Client application to navigate HAL resources on the server using high level
abstract Resource.

This project contains a few modules:

1. [hal-spel](https://github.com/C06A/HALSpeL/tree/develop/hal-spel): contains the wrapper itself
1. fuel-spel: provides some helpful functions to configure the underlying Fuel library
1. [go-about](https://github.com/C06A/HALSpeL/tree/develop/go-about): is the sample application using hal-spel to access information about many locations
1. [oxford](https://github.com/C06A/HALSpeL/tree/develop/oxford): is the sample application to access information about classes in Oxford
1. [geoIP](https://github.com/C06A/HALSpeL/tree/develop/geoIP): is the sample application to access information about IP-address from few different services

For more details read description of each module.

Also `oxford` and `geoIP` modules include implementations of Hi-/Low-level API from Java and Groovy code.
