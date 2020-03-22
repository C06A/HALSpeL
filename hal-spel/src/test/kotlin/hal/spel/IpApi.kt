package hal.spel

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.on

class IpApi: Spek({
    given("URL to ipify") {
        val url = "https://api.ipify.org{?format}"
        on("GET request") {
            halSpeL(url
            , templated = true
            ).GET("format" to "json"
            ).apply {
                println(status.code)
            }
        }
    }

    given("URL to MyIP") {
        val url = "https://api.myip.com"
        on("GET request") {
            halSpeL(url
            ).GET(

            ).apply {
                println(status.code)
            }
        }

    }
})
