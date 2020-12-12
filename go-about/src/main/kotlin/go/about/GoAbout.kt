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

        halSpeL("https://api.goabout.com", rel = "entry")
                .FETCH(aspect = LoggerFormatter(log::info, *(ReportPart.values())).makeAspect()
                ).apply {
                    println("\nVersion: ${this["version"]()}. Build: ${this["build"]()}")

                    FETCH("http://openid.net/specs/connect/1.0/issuer")

                    FETCH("http://rels.goabout.com/feedback")

                    FETCH("http://rels.goabout.com/health")
                            .apply {
                                println("Status: ${this["status"]<String>()}")
                            }
                    FETCH("http://rels.goabout.com/geocoder", "query" to "all"
                            , aspect = LoggerFormatter(log::warn, *ReportPart.values()).makeAspect()
                    )
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
