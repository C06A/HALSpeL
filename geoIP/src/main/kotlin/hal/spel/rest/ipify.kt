package hal.spel.rest

import hal.spel.*
import io.micronaut.http.HttpStatus

fun main(vararg args: String) {

    halSpeL("https://api.ipify.org{?format}"
            , templated = true
    ).apply {
        GET("format" to "json"
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println(body?.invoke())
            println(body?.let { it["ip"]() })
        }

        GET("format" to "text"
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println("body: ${result}")
        }
    }
    halSpeL("https://api6.ipify.org{?format}"
            , templated = true
    ).GET("format" to "json").apply {
        println("URL: ${request.url}")
        println(status.code)
        println(body?.invoke())
        println(body?.let { it["ip"]() })
    }
}
