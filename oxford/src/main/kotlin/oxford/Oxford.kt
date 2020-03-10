package oxford

import hal.spel.Answer
import hal.spel.FETCH
import hal.spel.Link
import hal.spel.aspect.*
import hal.spel.halSpeL
import io.micronaut.http.MediaType
import org.slf4j.LoggerFactory

class Oxford {
    companion object {

        val entry = "http://api.m.ox.ac.uk"

        val aspect: (Link.(Link.() -> Answer) -> Answer) = {
            Link("${entry}${this.href}"
                    , type = type
                    , templated = templated
            ).it()
        }

        val reporter: (String) -> Unit = { println(); println(it) }

        @JvmStatic
        fun main(vararg args: String) {
            halSpeL("/"
                    , type = MediaType.APPLICATION_HAL_JSON
                    , rel = "entryPoint"
            ).FETCH(aspect = makePostADocTagAspect(reporter
                    , *POST_PARTS.values()
                    , aspect = makePreADocTagAspect(reporter, PRE_PARTS.REL, PRE_PARTS.LINK, aspect = aspect))
            ).apply {
                FETCH("app:contacts")
                FETCH("app:courses"
                ).apply {
                    FETCH("hl:course", "id" to 10
                    ).FETCH("hl:subjects"
                    ).FETCH("courses:subject", 0)

                    FETCH("hl:search", "q" to "Russian")
                }

                FETCH("app:library")
            }
        }
    }
}
