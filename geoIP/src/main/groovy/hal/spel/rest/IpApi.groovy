package hal.spel.rest;

import com.github.kittinunf.fuel.core.Headers;
import hal.spel.Answer;
import hal.spel.HalKt;
import hal.spel.Link;

import java.util.HashMap;
import java.util.Map;

class IpApi {
    static void main(String... args) {
        Link entrance = HalKt.halSpeL('https://ipapi.co{/ip,field}', null, true, 'entry')

        Headers jsonHeaders = new Headers()
        jsonHeaders += [Accept: 'application/json']

        entrance.GET([ip: '8.8.8.8', field: 'json'], jsonHeaders).with {
            println("URL: ${it.request.url}")
            println(it.status.code)
            println(it.body?.invoke())
            println(it.body?.get("ip"))
        }

        entrance.GET([ip: '1.2.3.4', field: 'json'], jsonHeaders).with {
            println("URL: ${it.request.url}")
            println(it.status.code)
            println(it.body?.invoke())
            println(it.body?.get("ip"))
        }
    }
}
