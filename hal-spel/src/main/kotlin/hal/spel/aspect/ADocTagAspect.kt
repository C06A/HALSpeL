package hal.spel.aspect

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.core.requests.RepeatableBody
import hal.spel.Answer
import hal.spel.Link
import io.micronaut.http.HttpStatus
import kotlin.math.min

private val jackson = ObjectMapper()

private var counters = mutableMapOf<String, Int>()
private var relation: String? = null

private fun <T> formatTag(tagName: String, obj: T?, reporter: (String) -> Unit) {
    when (obj) {
        is Map<*, *> -> reporter("""
                |#tag::$relation-$tagName-${counters[relation]}[]
                |${obj.filterValues { it != null }.map { (key, value) ->
            "$key:\t$value"
        }.joinToString("\n|\t", "\t")}
                |#end::$relation-$tagName-${counters[relation]}[]
                """.trimMargin())
        is Collection<*> ->
            reporter("""
                |#tag::$relation-$tagName-${counters[relation]}[]
                |${obj.joinToString("\n|\t", "\t")}
                |#end::$relation-$tagName-${counters[relation]}[]
                """.trimMargin())
        else -> reporter("#tag::$relation-$tagName-${counters[relation]}[]"
                + "\n${obj ?: ""}"
                + "\n#end::$relation-$tagName-${counters[relation]}[]"
        )
    }
}

/**
 * AsciiDoc preRequest aspect definitions
 *
 * This Map defines the aspect making functions for different parts of [Link] object.
 *
 * The maker for [PRE_PARTS.REL] keeps track of similar requests and assigns unique number to make an AsciiDoc tag unique.
 * If this aspect won't be included in an aspect chain the [null] will be used instead of numbers and all segments
 * of AsciiDoc will have identical tags. It is recommended to always include [PRE_PARTS.REL] aspect even if the
 * corresponding segment won't ne used in the final documentation.
 *
 *
 */
val preADocTagFormatter = mapOf<PRE_PARTS, (Link, (String) -> Unit) -> Unit>(
        PRE_PARTS.REL to { link, reporter ->
            // Assign next available number to make a unique AsciiDoc tag
            relation = link.rel?.apply {
                counters[this] = counters[this]?.let {
                    it + 1
                } ?: 1
            }
            formatTag("ref", link.rel, reporter)
        },
        PRE_PARTS.LINK to { link, reporter ->
            formatTag("link", link.toMap(), reporter)
        },
        PRE_PARTS.URI to { link, reporter ->
            formatTag("URI", link.href, reporter)
        },
        PRE_PARTS.NAME to { link, reporter ->
            formatTag("name", link.name, reporter)
        },
        PRE_PARTS.TITLE to { link, reporter ->
            formatTag("title", link.title, reporter)
        },
        PRE_PARTS.TYPE to { link, reporter ->
            formatTag("type", link.type, reporter)
        }
)

val postADocTagFormatter = mapOf<POST_PARTS, (Answer, (String) -> Unit) -> Unit>(
        POST_PARTS.URL to { answer, reporter ->
            formatTag("URL", answer.response.url.toString(), reporter)
        },
        POST_PARTS.CURL to { answer, reporter ->
            formatTag("curl", answer.request.cUrlString(), reporter)
        },
        POST_PARTS.HEADERS_OUT to { answer, reporter ->
            formatTag("headerOut", answer.request.headers, reporter)
        },
        POST_PARTS.COOKIES_OUT to { answer, reporter ->
            formatTag("cookieOut", answer.request["Set-Cookies"], reporter)
        },
        POST_PARTS.BODY_OUT to { answer, reporter ->
            if (answer.request.body.isConsumed()) {
                formatTag("bodyOut", "Size: ${answer.request.body.length}", reporter)
            } else {
                answer.request.body.let {
                    if (it is RepeatableBody) {
                        if (it.toByteArray().size > 0) {
                            formatTag("bodyOut", "Size: ${it.toByteArray().size}", reporter)
                        } else {
                            formatTag("bodyOut", answer.request.body.asString(null), reporter)
                        }
                    } else {
                        formatTag("bodyOut", answer.request.body.asString(null), reporter)
                    }
                }
            }
        },
        POST_PARTS.STATUS to { answer, reporter ->
            formatTag("status", "${answer.status.code} (${answer.status})", reporter)
        },
        POST_PARTS.HEADERS_IN to { answer, reporter ->
            formatTag("headersIn", answer.response.headers, reporter)
        },
        POST_PARTS.COOKIES_IN to { answer, reporter ->
            formatTag("cookiesIn", answer.response["Cookies"], reporter)
        },
        POST_PARTS.BODY_IN to { answer, reporter ->
            formatTag("bodyIn", when (answer.status.code) {
                in (200..299) ->
                    if (answer.response.header(Headers.CONTENT_TYPE).any { it.contains("json") }) {
                        jackson.writerWithDefaultPrettyPrinter().writeValueAsString(answer.body)
                    } else {
                        val head = min(answer.response.contentLength, UNSTRUCTURED_HEAD_LENGTH)
                        "(length: ${answer.response.contentLength})\n${String(answer.response.data).substring(0, head.toInt())}"
                    }
                HttpStatus.FOUND.code -> "|Redirection to: ${answer.response.header("Location").first()}"
                else -> "${answer.body?.toJson()}"
            }, reporter)
        }
)

fun makePreADocTagAspect(reporter: (String) -> Unit, vararg parts: PRE_PARTS, aspect: Aspect? = null): Aspect {
    return makePreReporterAspect(reporter, preADocTagFormatter, *parts, aspect = aspect)
}

fun makePostADocTagAspect(reporter: (String) -> Unit, vararg parts: POST_PARTS, aspect: Aspect? = null): Aspect {
    return makePostReporterAspect(reporter, postADocTagFormatter, *parts, aspect = aspect)
}