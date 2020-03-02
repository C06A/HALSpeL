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

val preADocTagFormatter = mapOf<PRE_PARTS, (Link, (String) -> Unit) -> Unit>(
        PRE_PARTS.REL to { link, reporter ->
            relation = link.rel
            relation?.apply {
                counters[this] = counters[this]?.let {
                    it + 1
                } ?: 1
            }
        },
        PRE_PARTS.LINK to { link, reporter ->
            reporter("""
                |#tag::$relation-link-${counters[relation]}[]
                |${link.toMap().filterValues { it != null }.map { (key, value) ->
                "$key:\t$value"
            }.joinToString("\n|\t", "{", "\n|}")}
                |#end::$relation-link-${counters[relation]}[]
                """.trimMargin())
        },
        PRE_PARTS.URI to { link, reporter ->
            reporter("""
                #tag::$relation-URI-${counters[relation]}[]
                ${link.href}
                #end::$relation-URI-${counters[relation]}[]
                """.trimIndent())
        }
)

val postADocTagFormatter = mapOf<POST_PARTS, (Answer, (String) -> Unit) -> Unit>(
        POST_PARTS.URL to { answer, reporter ->
            reporter("""
                #tag::$relation-URL-${counters[relation]}[]
                ${answer.response.url}
                #end::$relation-URL-${counters[relation]}[]
                """.trimIndent())
        },
        POST_PARTS.CURL to { answer, reporter ->
            if (answer.request.method == Method.GET || answer.request.header(Headers.CONTENT_TYPE).contains("json")) {
                reporter("""
                |#tag::$relation-curl-${counters[relation]}[]
                |${answer.request.cUrlString()}
                |#end::$relation-curl-${counters[relation]}[]
                """.trimMargin())
            }
        },
        POST_PARTS.HEADERS_OUT to { answer, reporter ->
            reporter("""
                |#tag::$relation-headersOut-${counters[relation]}[]
                |${answer.request.headers.map { (key, value) ->
                "\t$key:\t${value.joinToString("\n|\t\t")}"
            }.joinToString("\n|")}
                |#end::$relation-headersOut-${counters[relation]}[]
                """.trimMargin())
        },
        POST_PARTS.COOKIES_OUT to { answer, reporter ->
            reporter("""
                |#tag::$relation-cookieOut-${counters[relation]}[]
                |${answer.request["Set-Cookies"].joinToString("\n|\t")}
                |#end::$relation-cookieOut-${counters[relation]}[]
                """.trimMargin())
        },
        POST_PARTS.BODY_OUT to { answer, reporter ->
            reporter("#tag::$relation-bodyOut-${counters[relation]}[]")
            if (answer.request.body.isConsumed()) {
                reporter("${answer.request.body.length}")
            } else {
                answer.request.body.let {
                    when (it) {
                        is RepeatableBody -> if(it.toByteArray().size > 0) {
                            reporter("Size: ${it.toByteArray().size}")
                        } else {
                            reporter("Body object: ${answer.request.body}")
                        }
                        else -> reporter("Body object: ${answer.request.body}")
                    }
                }
            }
            reporter("#end::$relation-bodyOut-${counters[relation]}[]")
        },
        POST_PARTS.STATUS to { answer, reporter ->
            reporter("""
                #tag::$relation-status-${counters[relation]}[]
                ${answer.status.code} (${answer.status})
                #end::$relation-status-${counters[relation]}[]
                """.trimIndent())
        },
        POST_PARTS.HEADERS_IN to { answer, reporter ->
            reporter("""
                |#tag::$relation-headersIn-${counters[relation]}[]
                |${answer.response.headers.map { (key, value) ->
                "\t$key:\t${value.joinToString("\n|\t")}"
            }.joinToString("\n|")}
                |#end::$relation-headersIn-${counters[relation]}[]
                """.trimMargin())
        },
        POST_PARTS.COOKIES_IN to { answer, reporter ->
            reporter("""
                |#tag::$relation-cookiesIn-${counters[relation]}[]
                |${answer.response["Cookies"].joinToString("\n|\t")}
                |#end::$relation-cookiesIn-${counters[relation]}[]
                """.trimMargin())
        },
        POST_PARTS.BODY_IN to { answer, reporter ->
            reporter("""
                |#tag::$relation-bodyIn-${counters[relation]}[]
                ${
            when (answer.status.code) {
                in (200..299) ->
                    if (answer.response.header(Headers.CONTENT_TYPE).any { it.contains("json") }) {
                        "|${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(answer.body)}"
                    } else {
                        val head = min(answer.response.contentLength, UNSTRUCTURED_HEAD_LENGTH)
                        "|(length: ${answer.response.contentLength})\n${String(answer.response.data).substring(0, head.toInt())}"
                    }
                HttpStatus.FOUND.code -> "|Redirection to: ${answer.response.header("Location").first()}"
                else -> "|${answer.body?.toJson()}"
            }}
                |#end::$relation-bodyIn-${counters[relation]}[]
                """.trimMargin())
        }
)

fun makePreADocTagAspect(reporter: (String) -> Unit, vararg parts: PRE_PARTS, aspect: Aspect? = null): Aspect {
    return makePreReporterAspect(reporter, preADocTagFormatter, *parts, aspect = aspect)
}

fun makePostADocTagAspect(reporter: (String) -> Unit, vararg parts: POST_PARTS, aspect: Aspect? = null): Aspect {
    return makePostReporterAspect(reporter, postADocTagFormatter, *parts, aspect = aspect)
}