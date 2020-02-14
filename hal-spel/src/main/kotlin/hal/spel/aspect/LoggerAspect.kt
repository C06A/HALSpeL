package hal.spel.aspect

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.extensions.cUrlString
import hal.spel.Answer
import hal.spel.Link
import io.micronaut.http.HttpStatus
import kotlin.math.min

const val UNSTRUCTURED_HEAD_LENGTH = 100L

private val jackson = ObjectMapper()

fun makePreLoggerAspect(logger: (String) -> Unit, aspect: Aspect? = null): Aspect {
    val linkLogger: LinkFun = {
        logger("--------------\n${if (name.isNullOrBlank()) {
            "Link href: $href"
        } else {
            "Link name: $name ($href)"
        }}")
    }

    return makePreReporterAspect(
            linkLogger
            , aspect = makeDefaultAspectIfNull(aspect)
    )
}

val postLoggerFormatter = mapOf<CONN_PARTS, (Answer, (String) -> Unit) -> Unit>(
        CONN_PARTS.URL to { answer, logger -> logger("URL: ${answer.request.url}") },
        CONN_PARTS.LINK to { answer, logger -> logger("Resource Location: ${answer.response.url}") },
        CONN_PARTS.CURL to { answer, logger ->
            if (answer.request.method == Method.GET || answer.request.header(Headers.CONTENT_TYPE).contains("json")) {
                logger("$> ${answer.request.cUrlString()}")
            }
        },
        CONN_PARTS.HEADERS_OUT to { answer, logger ->
            logger(
                    "Header sent: ${answer.request.headers.map { (key, value) ->
                        "\t$key:\t${value.joinToString("\n\t\t")}"
                    }.joinToString("\n", "\n")}"
            )
        },
        CONN_PARTS.COOKIES_OUT to { answer, logger ->
            logger("Cookies sent: ${answer.request["Set-Cookies"].joinToString("\n\t", "\n\t")}")
        },
        CONN_PARTS.BODY_OUT to { answer, logger ->
            if (answer.request.method in setOf(Method.POST, Method.PUT, Method.PATCH))
            logger(
                    if (answer.request.body.isConsumed()) {
                        "Length of the sent Body: ${answer.request.body.length}"
                    } else {
                        "Body sent:\n${answer.request.body}"
                    }
            )
        },
        CONN_PARTS.STATUS to { answer, logger -> logger("Status: ${answer.status.code} (${answer.status})") },
        CONN_PARTS.HEADERS_IN to { answer, logger ->
            logger(
                    "Headers received: ${answer.response.headers.map { (key, value) ->
                        "\t$key:\t${value.joinToString("\n\t\t")}"
                    }.joinToString("\n", "\n")}"
            )
        },
        CONN_PARTS.COOKIES_IN to { answer, logger ->
            logger("Cookies received: ${answer.response["Cookies"].joinToString("\n\t", "\n\t")}")
        },
        CONN_PARTS.BODY_IN to { answer, logger ->
            logger( "Body received:${
                    when (answer.status.code) {
                        in (200..299) ->
                            if (answer.response.header(Headers.CONTENT_TYPE).any { it.contains("json") }) {
                                "\n${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(answer.body)}"
                            } else {
                                val head = min(answer.response.contentLength, UNSTRUCTURED_HEAD_LENGTH)
                                " (length: ${answer.response.contentLength})\n${String(answer.response.data).substring(0, head.toInt())}"
                            }
                        HttpStatus.FOUND.code -> "Redirection to: ${answer.response.header("Location").first()}"
                        else -> "\n${answer.body?.toJson()}"
                    }}"
            )
        }
)

fun makePostLoggerAspect(logger: (String) -> Unit, vararg parts: CONN_PARTS, aspect: Aspect? = null): Aspect {

    return makePostReporterAspect(
            *(parts.map {
                { answer: Answer ->
                    postLoggerFormatter[it]?.invoke(answer, logger) ?: Unit
                }
            }.toTypedArray())
            , aspect = makeDefaultAspectIfNull(aspect)
    )
}
