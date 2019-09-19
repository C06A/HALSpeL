package hal.spel

import com.github.kittinunf.fuel.core.*
import io.micronaut.http.HttpStatus
import java.io.File
import java.lang.IllegalArgumentException
import com.google.gson.Gson
import com.helpchoice.kotlin.koton.KotON
import com.helpchoice.kotlin.koton.kotON
import java.io.InvalidObjectException
import java.lang.Exception

const val ACTIONS = "_links"
const val RESOURCES = "_embedded"

private val SIMPLE_ASPECT: Link.(Link.() -> Answer) -> Answer = { it() }


fun Link.FETCH(
        vararg params: Pair<String, Any?>
        , headers: Headers?
        , aspect: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
        , handler: (Answer.() -> Unit)? = null
): Resource {
    return FETCH(params.toMap(), headers, aspect, handler)
}

fun Link.FETCH(
        params: Map<String, Any?>
        , headers: Headers?
        , aspect: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
        , after: (Answer.() -> Unit)? = null
): Resource {
    val answer = aspect {
        GET(params, headers = headers)
    }
    after?.let {
        answer.after()
    }
    return answer().let {
        Resource(it, headers ?: Headers(), aspect)
    }
}

//fun String?.TAKE(
//        vararg params: Pair<String, Any?>
//        , type: String = "application/hal+json"
//        , headers: Headers? = null
//        , submitter: (Link.(Link.() -> Answer) -> Answer) = SUBMITTER
//        , after: (Answer.() -> Unit)? = null
//): Resource {
//    return TAKE(params.toMap(), type, headers, submitter, after)
//}
//
//fun String?.TAKE(
//        params: Map<String, Any?>
//        , type: String = "application/hal+json"
//        , headers: Headers? = null
//        , submitter: (Link.(Link.() -> Answer) -> Answer) = SUBMITTER
//        , after: (Answer.() -> Unit)? = null
//): Resource {
//    val link = Link(
//            this ?: "http://localhost:8080"
//            , type
//            , params?.let { true }
//    )
//
//    val header: Headers = Headers().apply {
//        put("Accept", listOf(type))
//        headers?.let {
//            putAll(headers as Map<String, HeaderValues>)
//        }
//    }
//    return link.TAKE(params, headers = header, submitter = submitter, after = after)
//}

fun Link.CREATE(
        vararg params: Pair<String, Any?>
        , headers: Headers?
        , body: String = ""
        , submitter: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
        , after: (Answer.() -> Unit)? = null
): Resource? {
    return CREATE(params.toMap(), headers, body, submitter, after)
}

fun Link.CREATE(
        params: Map<String, Any?>
        , headers: Headers?
        , body: String = ""
        , submitter: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
        , after: (Answer.() -> Unit)? = null
): Resource? {
    val answer = submitter {
        POST(params, headers = headers, body = body)
    }
    after?.let {
        answer.after()
    }
    return answer()?.let {
        Resource(it, headers ?: Headers(), submitter)
    } ?: null
}

//fun String?.PLACE(
//        vararg params: Pair<String, Any?>
//        , type: String = "application/hal+json"
//        , headers: Headers? = null
//        , body: String = ""
//        , submitter: (Link.(Link.() -> Answer) -> Answer) = SUBMITTER
//        , after: (Answer.() -> Unit)? = null
//): Resource {
//    return PLACE(params.toMap(), type, headers, body, submitter, after)
//}
//
//fun String?.PLACE(
//        params: Map<String, Any?>
//        , type: String = "application/hal+json"
//        , headers: Headers? = null
//        , body: String = ""
//        , submitter: (Link.(Link.() -> Answer) -> Answer) = SUBMITTER
//        , after: (Answer.() -> Unit)? = null
//): Resource {
//    val link = Link(
//            this ?: "http://localhost:8080"
//            , type
//            , params?.let { true }
//    )
//
//    val header: Headers = Headers().apply {
//        put("Accept", listOf(type))
//        headers?.let {
//            putAll(headers as Map<String, HeaderValues>)
//        }
//    }
//    return link.PLACE(params, headers = header, body = body, submitter = submitter, after = after)
//}


class Resource(
        val kjson: KotON<Any>
        , private val headers: Headers = Headers()
        , var aspect: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
) {
    operator fun invoke(rel: String, index: Int? = null): Resource = Resource(
            index?.let {
                kjson[RESOURCES, rel][index]
            } ?: kjson[RESOURCES, rel]
            , headers
            , aspect
    )

    val references: Collection<String>
        inline get() = kjson<Map<String, Any>>(ACTIONS)?.run {
            keys - "self"
        } ?: emptySet()

    val resources: Collection<String>
        inline get() = kjson<Map<String, Any>>(RESOURCES)?.keys ?: emptySet()

    val attributes: Collection<String>
        inline get() = kjson<Map<String, Any>>()?.run {
            keys - ACTIONS - RESOURCES
        } ?: emptySet()

    inline operator fun get(index: Int): KotON<Any> = kjson[index]

    inline operator fun get(rel: String): KotON<Any> {
        return when (rel) {
            ACTIONS, RESOURCES -> throw IllegalArgumentException("There are no attribute with name '$rel'")
            else -> kjson[rel]
        }
    }

    operator fun String.plus(value: String) {
        val values = headers[this]
        headers.put(this, values + value)
    }

    operator fun String.unaryMinus() {
        headers - this
    }

    operator fun String.minus(value: String) {
        var values = headers[this]
        if (values != null) {
            values -= value
            if (values.isEmpty()) {
                headers.remove(this)
            }
        }

    }

    override fun toString(): String {
        return """
            |${mutableListOf("References:").also { it.addAll(references) }.joinToString("\n\t")}
            |${mutableListOf("Resources:").also { it.addAll(resources) }.joinToString("\n\t")}
            |${mutableListOf("Attributes:").also { it.addAll(attributes) }.joinToString("\n\t")}
        """.trimMargin()
    }

    private fun getLink(rel: String, index: Int? = null): Link {
        val links = kjson[ACTIONS]
        val json = links[rel]
        return Link(json)
    }

    private fun Answer.execute(after: (Answer.() -> Unit)? = null): Resource? {
        after?.let {
            this.after()
        }
        return this()?.let {
            Resource(it, headers, aspect)
        } ?: null
    }

    fun FETCH(link: String? = null, vararg params: Pair<String, Any?>, after: (Answer.() -> Unit)? = null): Resource {
        return FETCH(link, params.toMap(), after)
    }

    fun FETCH(link: String? = null, params: Map<String, Any?>, after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link ?: "self").FETCH(params, headers = headers, aspect = aspect, after = after)
    }

    fun CREATE(link: String, vararg params: Pair<String, Any?>, body: String, after: (Answer.() -> Unit)? = null): Resource {
        return CREATE(link, params.toMap(), body, after)
    }

    fun CREATE(link: String, params: Map<String, Any?>, body: String, after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { POST(params, headers = headers, body = body) }
                .execute(after)
                ?: Resource(kotON())
    }

    fun CREATE(link: String, params: Map<String, Any?>, source: BodySource, length: BodyLength, after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { POST(params, headers = headers, source = source, length = length) }
                .execute(after)
                ?: Resource(kotON())
    }

    fun REPLACE(link: String, vararg params: Pair<String, Any?>, body: String, after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { PUT(*params, headers = headers, body = body) }
                .execute(after)
                ?: Resource(kotON())
    }

    fun REPLACE(link: String, vararg params: Pair<String, Any?>, source: BodySource, length: BodyLength, after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { PUT(*params, headers = headers, source = source, length = length) }
                .execute(after)
                ?: Resource(kotON())
    }

    fun CREATE(link: String, vararg params: Pair<String, Any?>, file: File, after: (Answer.() -> Unit)? = null): Resource {
        return CREATE(link, *params, files = mapOf(file.name to file), after = after)
    }

    fun CREATE(link: String, vararg params: Pair<String, Any?>, files: Collection<File>, after: (Answer.() -> Unit)? = null): Resource {
        return CREATE(link, *params, files = files.map {
            it.name to it
        }.toMap(), after = after)
    }

    fun CREATE(link: String, vararg params: Pair<String, Any?>, files: Map<String, File>, after: (Answer.() -> Unit)? = null): Resource {
//        return PLACE(link, params.toMap(), files, after)
//    }
//
//    fun PLACE(link: String, params: Map<String, Any?>, files: Map<String, File>, after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { UPLOAD(*params, headers = headers, files = files) }
                .execute(after)
                ?: Resource(kotON())
    }

    fun UPDATE(link: String, params: Map<String, Any?> = emptyMap(), body: String, after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { PATCH(headers = headers, body = body) }
                .execute(after)
                ?: Resource(kotON())
    }

    fun FETCH(link: String, params: Map<String, Any?> = emptyMap(), folder: File, after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { DOWNLOAD(headers = headers, file = folder) }
                .execute(after)
                ?: Resource(kotON())
    }

    fun REMOVE(link: String, params: Map<String, Any?> = emptyMap(), after: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { DELETE(headers = headers) }
                .execute(after)
                ?: Resource(kotON())
    }
}

class Answer(
        private val exchange: ResponseResultOf<String>
) {
    val request
        get() = exchange.first

    val response
        get() = exchange.second

    val result
        get() = exchange.third

    val body: Map<String, Any?>?
        get() {
            val (success, failure) = result
            return success?.let {
                try {
                    Gson().fromJson<Map<String, Any?>>(it, Map::class.java) as Map<String, Any?>
                } catch (e: Exception) {
                    throw InvalidObjectException("${e.javaClass.canonicalName} (${e.message})")
                }
            } ?: failure?.let {
                Gson().fromJson(String(failure.errorData), Map::class.java) as Map<String, Any?>
            }
        }

    val error: FuelError?
        get() {
            val (_, error) = result
            return error
        }

    val status: HttpStatus
        get() {
            return HttpStatus.valueOf(response.statusCode)
        }

    operator fun invoke(): KotON<Any> {
        return body?.let {
            kotON(it)
        } ?: kotON { "" } // { null }
    }

    fun HttpStatus.getMessage(): String {
        return "$code ($reason)"
    }

    fun RERUN(vararg header: Pair<String, String>): Answer {
        val link = Link(request.url.toExternalForm())
        val headers = Headers.from(*header).apply {
            putAll(request.headers)
        }
        return when (request.method) {
            Method.POST -> link.POST(headers = headers, source = { request.body.toStream() }, length = { request.body.toByteArray().size.toLong() })
            Method.PUT -> link.PUT(headers = headers, source = { request.body.toStream() }, length = { request.body.toByteArray().size.toLong() })
            Method.PATCH -> link.PATCH(headers = headers, source = { request.body.toStream() }, length = { request.body.toByteArray().size.toLong() })
            Method.DELETE -> link.DELETE(headers = headers)
            else -> link.GET(emptyMap(), headers)
        }
    }
}
