package hal.spel.rest

import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.cUrlString
import hal.spel.*
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType

fun main(vararg args: String) {

    halSpeL("https://api.ipify.org{?format}"
            , templated = true
    ).apply {
        GET("format" to "json"
            , headers = Headers() + ("Accept" to MediaType.APPLICATION_JSON)
        ).apply {
            println("> ${request.cUrlString()}")
            println(status.code)
            println(body?.invoke())
            println(body?.let { it["ip"]() })
        }

        GET("format" to "text"
            , headers = Headers() + ("Accept" to MediaType.TEXT_PLAIN)
        ).apply {
            println("> ${request.cUrlString()}")
            println(status.code)
            println("body: ${result}")
        }
    }
    halSpeL("https://api6.ipify.org{?format}"
            , templated = true
    ).GET("format" to "json"
        , headers = Headers() + ("Accept" to MediaType.APPLICATION_JSON)
    ).apply {
        println("> ${request.cUrlString()}")
//        println(status.code)
        println(body?.let {
            it()
        })
        println(body?.let {
            it["ip"]()
        })
    }
}
