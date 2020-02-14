package oxford

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.extensions.cUrlString
import hal.spel.Answer
import hal.spel.FETCH
import hal.spel.Link
import hal.spel.aspect.AnswerFun
import hal.spel.aspect.CONN_PARTS
import hal.spel.aspect.makePostLoggerAspect
import hal.spel.aspect.makePreLoggerAspect
import io.micronaut.http.MediaType

class Oxford {
    companion object {
        val jackson = ObjectMapper()

        val entry = "http://api.m.ox.ac.uk"

        val aspect: (Link.(Link.() -> Answer) -> Answer) = {
            Link("${entry}${this.href}"
                    , type = type
                    , templated = templated
            ).it()
        }

        val logger: (String) -> Unit = { println(); println(it) }

        @JvmStatic
        fun main(vararg args: String) {
            Link("/"
                    , type = MediaType.APPLICATION_HAL_JSON
            ).FETCH(aspect = makePostLoggerAspect(logger, CONN_PARTS.URL, CONN_PARTS.BODY_OUT, CONN_PARTS.STATUS, CONN_PARTS.BODY_IN, aspect = makePreLoggerAspect(logger, aspect))
            ).apply {
                FETCH("app:contacts")
                FETCH("app:courses"
                ).apply {
                    FETCH("hl:course", "id" to 10)
                }.apply {
                    FETCH("hl:subjects"
                    ).FETCH("courses:subject", 0)
                }.apply {
                    FETCH("hl:search", "q" to "Russian")
                }

                FETCH("app:library")
            }
        }
    }
}