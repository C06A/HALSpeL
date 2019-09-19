# HALSpeL

Kotlin DSL wrapper for Fuel to work with HAL-API applications as set of Resources.

This wrapper provides DSL allowing Client application to navigate HAL resources on the server using high level
abstract Resource.

The HAL standard is an extension of the REST API. This means the each URL points to the Resource, which can be
created, retrieved, updated and removed. The same applieds to HAL.

In contrast to REST fully implemeted HAL standard allows Client to do not "know" any server's endpoints (URLs),
but discover them from other Resources. The only endpoint Client should have is the main entry point -- base
URL to the server. That endpoint will return the first Resource, which contains links to other Resources for
Client to switch to.


