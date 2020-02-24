package go.about

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.extensions.cUrlString
import fuel.spel.configAsHostnameVerifier
import fuel.spel.configSslTrust
import hal.spel.*
import hal.spel.aspect.*
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory

fun main(vararg args: String) {
    GoAbout().test()
}

val jackson = ObjectMapper()

class GoAbout {
    val log = LoggerFactory.getLogger(this::class.java)
    val reporter: (String) -> Unit = { log.info(it) }

    fun test() {
        println("Documentation: https://apidocs.goabout.com")
        println()

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
                when (status.code) {
                    in (200..299) -> println("Body:\n${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(body)}")
                    HttpStatus.FOUND.code -> {
                        println("Redirection to: ${this.response.header("Location").first()}")
                    }
                    else -> {
                        println("Body: $body")
                    }
                }
            }
        }

        halSpeL("https://api.goabout.com", rel = "entry")
                .FETCH(aspect = makePostReporterAspect(reporter
                        , postLoggerFormatter
                        , *(POST_PARTS.values().filter { it == POST_PARTS.BODY_OUT }.toTypedArray())
                        , aspect = makePreReporterAspect(reporter, preLoggerFormatter, *PRE_PARTS.values())))
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
