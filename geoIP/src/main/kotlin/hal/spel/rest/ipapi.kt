package hal.spel.rest

import com.github.kittinunf.fuel.core.Headers
import hal.spel.*
import io.micronaut.http.HttpStatus

fun main(vararg args: String) {
    var myId: String? = null

    halSpeL("https://ipapi.co{/ip,field}"
            , templated = true
    ).apply {
        GET(
                "ip" to "8.8.8.8"
                , "field" to "json"
                , headers = Headers()
                + ("Accept" to "application/json")
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println(body?.invoke())
            println(body?.let { it["ip"]() })
        }

        GET(
                "ip" to "1.2.3.4"
                , "field" to "json"
                , headers = Headers()
                + ("Accept" to "application/json")
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println(body?.invoke())
            println(body?.let { it["ip"]() })
        }
    }
}
