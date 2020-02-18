package oxford

import hal.spel.Answer
import hal.spel.FETCH
import hal.spel.Link
import hal.spel.aspect.*
import io.micronaut.http.MediaType
import org.slf4j.LoggerFactory

class Oxford {
    companion object {
        val log = LoggerFactory.getLogger(this::class.java)

        val entry = "http://api.m.ox.ac.uk"

        val aspect: (Link.(Link.() -> Answer) -> Answer) = {
            Link("${entry}${this.href}"
                    , type = type
                    , templated = templated
            ).it()
        }

        val logger: (String) -> Unit = { println(); println(it) }
//        val logger: (String) -> Unit = { log.info(it) }

        @JvmStatic
        fun main(vararg args: String) {
            Link("/"
                    , type = MediaType.APPLICATION_HAL_JSON
            ).FETCH(aspect = makePostReporterAspect(logger
                    , postLoggerFormatter
                    , POST_PARTS.URL, POST_PARTS.BODY_OUT, POST_PARTS.STATUS // , CONN_PARTS.BODY_IN
                    , aspect = makePreReporterAspect(logger, preLoggerFormatter, PRE_PARTS.LINK, aspect = aspect))
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
