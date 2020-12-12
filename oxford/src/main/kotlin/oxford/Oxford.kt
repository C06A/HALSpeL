package oxford

import hal.spel.Answer
import hal.spel.FETCH
import hal.spel.Link
import hal.spel.aspect.*
import hal.spel.halSpeL
import io.micronaut.http.MediaType
import org.slf4j.LoggerFactory
import java.io.File

class Oxford {
    companion object {

        val entry = "http://api.m.ox.ac.uk"

        val aspect: (Link.(Link.() -> Answer) -> Answer) = {
            Link("${entry}${this.href}"
                    , type = type
                    , templated = templated
            ).it()
        }

        // tag::stdoutReporter[]
        val reporterLoger: (String) -> Unit = { println(); println(it) }
        // end::stdoutReporter[]

        val folderPath = "oxford/build/asciidoc"
        val folderFile = File(folderPath).apply { mkdirs() }
        val fileName = "Oxford-includes.asciidoc"

        // tag::fileReporter[]
        val fileWriter = File(folderFile, fileName).printWriter()
        val reporter: (String) -> Unit = {
            fileWriter.println(it)
        }
        // end::fileReporter[]

        @JvmStatic
        fun main(vararg args: String) {
            halSpeL("/"
                    , type = MediaType.APPLICATION_HAL_JSON
                    , rel = "entryPoint"
            ).apply {
                FETCH(aspect = makePreADocTagAspect(reporter
                        , PRE_PARTS.REL, PRE_PARTS.LINK
                        , aspect = makePostADocTagAspect(reporter
                        , *POST_PARTS.values()
                        , aspect = makePostLoggerAspect(reporterLoger
                        , POST_PARTS.CURL
                        , aspect = makePreLoggerAspect(reporterLoger
                        , PRE_PARTS.URI, aspect = aspect)
                )))
                ).FETCH("app:contacts")

                FETCH(aspect = makePostADocTagAspect(reporter
                        , *POST_PARTS.values()
                        , aspect = makePreADocTagAspect(reporter, PRE_PARTS.REL, PRE_PARTS.LINK
                        , aspect = aspect))
                ).apply {
                    FETCH("app:courses"
                    ).apply {
                        FETCH("hl:course", "id" to 10)

                        FETCH("hl:subjects"
                        ).FETCH("courses:subject", 0)

                        FETCH("hl:search", "q" to "Russian")
                    }

                    FETCH("app:library")
                }
            }
        }
    }
}
