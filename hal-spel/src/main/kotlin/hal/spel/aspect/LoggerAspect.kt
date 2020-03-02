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

val preLoggerFormatter = mapOf<PRE_PARTS, (Link, (String) -> Unit) -> Unit>(
        PRE_PARTS.LINK to { link, reporter ->
            reporter("--------------\n${if (link.name.isNullOrBlank()) {
                "Link href: ${link.href}"
            } else {
                "Link name: ${link.name} (${link.href})"
            }}")
        },
        PRE_PARTS.URI to { link, reporter -> reporter("URL: ${link.href}") },
        PRE_PARTS.NAME to { link, reporter -> reporter("Named: ${link.name}")},
        PRE_PARTS.TITLE to { link, reporter -> reporter("Titled: ${link.title}")},
        PRE_PARTS.TYPE to { link, reporter -> reporter("Accept: ${link.type}")}
)

val postLoggerFormatter = mapOf<POST_PARTS, (Answer, (String) -> Unit) -> Unit>(
        POST_PARTS.URL to { answer, reporter -> reporter("Resource Location: ${answer.response.url}") },
        POST_PARTS.CURL to { answer, reporter ->
            if (answer.request.method == Method.GET || answer.request.header(Headers.CONTENT_TYPE).contains("json")) {
                reporter("$> ${answer.request.cUrlString()}")
            }
        },
        POST_PARTS.HEADERS_OUT to { answer, reporter ->
            reporter(
                    "Header sent: ${answer.request.headers.map { (key, value) ->
                        "\t$key:\t${value.joinToString("\n\t\t")}"
                    }.joinToString("\n", "\n")}"
            )
        },
        POST_PARTS.COOKIES_OUT to { answer, reporter ->
            reporter("Cookies sent: ${answer.request["Set-Cookies"].joinToString("\n\t", "\n\t")}")
        },
        POST_PARTS.BODY_OUT to { answer, reporter ->
            if (answer.request.method in setOf(Method.POST, Method.PUT, Method.PATCH))
            reporter(
                    if (answer.request.body.isConsumed()) {
                        "Length of the sent Body: ${answer.request.body.length}"
                    } else {
                        "Body sent:\n${answer.request.body}"
                    }
            )
        },
        POST_PARTS.STATUS to { answer, reporter -> reporter("Status: ${answer.status.code} (${answer.status})") },
        POST_PARTS.HEADERS_IN to { answer, logger ->
            logger(
                    "Headers received: ${answer.response.headers.map { (key, value) ->
                        "\t$key:\t${value.joinToString("\n\t\t")}"
                    }.joinToString("\n", "\n")}"
            )
        },
        POST_PARTS.COOKIES_IN to { answer, reporter ->
            reporter("Cookies received: ${answer.response["Cookies"].joinToString("\n\t", "\n\t")}")
        },
        POST_PARTS.BODY_IN to { answer, reporter ->
            reporter( "Body received:${
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

fun makePreLogger(reporter: (String) -> Unit, vararg parts: PRE_PARTS, aspect: Aspect? = null): Aspect {
    return makePreReporterAspect(reporter, preLoggerFormatter, *parts, aspect = aspect)
}

fun makePostLogger(reporter: (String) -> Unit, vararg parts: POST_PARTS, aspect: Aspect? = null): Aspect {
    return makePostReporterAspect(reporter, postLoggerFormatter, *parts, aspect = aspect)
}
