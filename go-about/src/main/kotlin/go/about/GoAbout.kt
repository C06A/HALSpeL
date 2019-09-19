package go.about

import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.google.gson.GsonBuilder
import fuel.spel.configAsHostnameVerifier
import fuel.spel.configSslTrust
import hal.spel.Link
import io.micronaut.http.HttpStatus
import hal.spel.FETCH
import hal.spel.Resource

fun main(vararg args: String) {
    GoAbout().test()
}

class GoAbout {
    fun test() {
        println("Documentation: https://apidocs.goabout.com")

        "SSL".configSslTrust()
        null.configAsHostnameVerifier()

        Link("https://api.goabout.com", templated = false)
                .FETCH(headers = null, aspect = {
                    it().apply {
                        println(request.cUrlString())
                        println("Status: ${status.code} ($status)")
                        println("Body:\n${GsonBuilder().setPrettyPrinting().create().toJson(body)}")
                        println()
                        println(Resource(this()))
                    }
                })
                .apply {
                    println("\nVersion: ${this["version"]}. Build: ${this["build"]}")
                    println("\n======================\n")
                    println("\nissuer:")
                    try {
                        FETCH("http://openid.net/specs/connect/1.0/issuer")
                    } catch (e: Throwable) {
                        println("Exception: ${e::class.java.canonicalName}: ${e.localizedMessage}")
                    }
                    println("\n======================\n")
                    println("health:")
                    FETCH("http://rels.goabout.com/health"
                    ).apply {
                        println("Status: ${this["status"]<String>()}")
                    }
                    println("\n======================\n")
                    println("geocoder:")
                    FETCH("http://rels.goabout.com/geocoder", "query" to "all"
                    ).apply {
                        println("\nlocation object:")
                        println(this("http://rels.goabout.com/location", 0))
                        println("\nRefreshed location:")
                        println(this("http://rels.goabout.com/location", 0).FETCH())
                    }
                    println("\n======================\n")
                    println("order-user-property-schema:")
                    FETCH("http://rels.goabout.com/order-user-property-schema")
                }
    }
}
