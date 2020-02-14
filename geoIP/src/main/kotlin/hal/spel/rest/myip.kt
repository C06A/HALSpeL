package hal.spel.rest

import hal.spel.*
import io.micronaut.http.HttpStatus

fun main(vararg args: String) {
    var myId: String? = null

    halSpeL("https://api.myip.com").GET().apply {
        println("URL: ${request.url}")
        println(status.code)
        println(body?.invoke())
        println(body?.let { it["ip"]() })
    }
}
