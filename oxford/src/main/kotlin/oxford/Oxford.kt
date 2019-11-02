package oxford

import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.google.gson.GsonBuilder
import hal.spel.Answer
import hal.spel.FETCH
import hal.spel.Link
import io.micronaut.http.MediaType

class Oxford {
    companion object {
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
                println("Body:\n${GsonBuilder().setPrettyPrinting().create().toJson(body)}")
            }.apply {
                FETCH("app:contacts")
                FETCH("app:courses"
                ) {
                    println("Status: ${status.code} ($status)")
                    println("Body:\n${GsonBuilder().setPrettyPrinting().create().toJson(body)}")
                }.apply {
                    FETCH("hl:course", "id" to 10) {
                        println("Status: ${status.code} ($status)")
                        println("Body:\n${GsonBuilder().setPrettyPrinting().create().toJson(body)}")
                    }
                }.apply {
                    FETCH("hl:subjects"
                    ).FETCH("courses:subject", 0)
                }.apply {
                    FETCH("hl:search", "q" to "Russian") {
                        println("Status: ${status.code} ($status)")
                        println("Body:\n${GsonBuilder().setPrettyPrinting().create().toJson(body)}")
                    }
                }

                FETCH("app:library")
            }
        }
    }
}