package go.about

import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.google.gson.GsonBuilder
import fuel.spel.configAsHostnameVerifier
import fuel.spel.configSslTrust
import hal.spel.*
import io.micronaut.http.HttpStatus

fun main(vararg args: String) {
    GoAbout().test()
}

class GoAbout {
    fun test() {
        println("Documentation: https://apidocs.goabout.com")

        "SSL".configSslTrust()
        null.configAsHostnameVerifier()

        val aopLog: (Link.(Link.() -> Answer) -> Answer) = {
            println()
            if (name.isNullOrBlank()) {
                println("Link href: $href")
            } else {
                println("Link name: $name ($href)")
            }
            it().apply {
                println(request.cUrlString())
                println("Status: ${status.code} ($status)")
                when(status.code) {
                    in(200..299) -> println("Body:\n${GsonBuilder().setPrettyPrinting().create().toJson(body)}")
                    HttpStatus.FOUND.code -> {
                        println("Redirection to: ${this.response.header("Location").first()}")
                    }
                    else -> {
                        println("Body: $body")
                    }
                }
            }
        }

        halSpeL("https://api.goabout.com")
                .FETCH(aspect = aopLog)
                .apply {
                    println("\nVersion: ${this["version"]()}. Build: ${this["build"]()}")
//                    FETCH("http://openid.net/specs/connect/1.0/issuer")
                    FETCH("http://rels.goabout.com/feedback")
                    FETCH("http://rels.goabout.com/health")
                            .apply {
                                println("Status: ${this["status"]<String>()}")
                            }
                    FETCH("http://rels.goabout.com/geocoder", "query" to "all")
                            .apply {
                                println("\nlocation object:")
                                println(this("http://rels.goabout.com/location", 0))
                                println("\nRefreshed location:")
                                println(this("http://rels.goabout.com/location", 0)
                                        .FETCH())
                            }
                    FETCH("http://rels.goabout.com/order-user-property-schema")
                }
    }
}
