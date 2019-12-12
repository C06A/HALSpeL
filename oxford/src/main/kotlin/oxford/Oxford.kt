package oxford

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.extensions.cUrlString
import hal.spel.Answer
import hal.spel.FETCH
import hal.spel.Link
import io.micronaut.http.MediaType

class Oxford {
    companion object {
        val jackson = ObjectMapper()

        val entry = "http://api.m.ox.ac.uk"

        val aspect: (Link.(Link.() -> Answer) -> Answer) = {
            Link("${entry}${this.href}"
                    , type = type
                    , templated = templated
            ).apply {
                println()
                println("URL: $href")
            }.it().apply {
                println("$> ${request.cUrlString()}")
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            Link("/"
                    , type = MediaType.APPLICATION_HAL_JSON
            ).FETCH(aspect = aspect) {
                println("Status: ${status.code} ($status)")
                println("Body:\n${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(body)}")
            }.apply {
                FETCH("app:contacts")
                FETCH("app:courses"
                ) {
                    println("Status: ${status.code} ($status)")
                    println("Body:\n${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(body)}")
                }.apply {
                    FETCH("hl:course", "id" to 10) {
                        println("Status: ${status.code} ($status)")
                        println("Body:\n${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(body)}")
                    }
                }.apply {
                    FETCH("hl:subjects"
                    ).FETCH("courses:subject", 0)
                }.apply {
                    FETCH("hl:search", "q" to "Russian") {
                        println("Status: ${status.code} ($status)")
                        println("Body:\n${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(body)}")
                    }
                }

                FETCH("app:library")
            }
        }
    }
}