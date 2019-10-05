package hal.spel

import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.result.getAs
import io.micronaut.http.HttpStatus
import java.io.File
import java.lang.IllegalArgumentException
import com.google.gson.Gson
import com.helpchoice.kotlin.koton.KotON
import com.helpchoice.kotlin.koton.kotON
import java.lang.Exception
import java.util.*

const val ACTIONS = "_links"
const val RESOURCES = "_embedded"

private val SIMPLE_ASPECT: Link.(Link.() -> Answer) -> Answer = { it() }


fun Link.FETCH(
        vararg params: Pair<String, Any?>
        , headers: Headers? = null
        , aspect: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
        , handler: (Answer.() -> Unit)? = null
): Resource {
    return FETCH(params.toMap(), headers, aspect, handler)
}

fun Link.FETCH(
        params: Map<String, Any?>
        , headers: Headers? = null
        , aspect: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
        , handler: (Answer.() -> Unit)? = null
): Resource {
    val answer = aspect {
        GET(params, headers = headers)
    }
    handler?.let {
        answer.handler()
    }
    return answer().let {
        Resource(it, aspect)
    }
}

fun Link.CREATE(
        vararg params: Pair<String, Any?>
        , headers: Headers?
        , body: String = ""
        , aspect: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
        , tail: (Answer.() -> Unit)? = null
): Resource? {
    return CREATE(params.toMap(), headers, body, aspect, tail)
}

fun Link.CREATE(
        params: Map<String, Any?>
        , headers: Headers?
        , body: String = ""
        , aspect: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
        , tail: (Answer.() -> Unit)? = null
): Resource? {
    val answer = aspect {
        POST(params, headers = headers, body = body)
    }
    tail?.let {
        answer.tail()
    }
    return answer()?.let {
        Resource(it, aspect)
    } ?: null
}

class Resource(
        val koton: KotON<Any>
        , var aspect: (Link.(Link.() -> Answer) -> Answer) = SIMPLE_ASPECT
) {
    operator fun invoke(rel: String, index: Int? = null): Resource = Resource(
            index?.let {
                koton[RESOURCES, rel][index]
            } ?: koton[RESOURCES, rel]
            , aspect
    )

    val references: Collection<String>
        inline get() = koton<Map<String, Any>>(ACTIONS)?.run {
            keys - "self"
        } ?: emptySet()

    val resources: Collection<String>
        inline get() = koton<Map<String, Any>>(RESOURCES)?.keys ?: emptySet()

    val attributes: Collection<String>
        inline get() = koton<Map<String, Any>>()?.run {
            keys - ACTIONS - RESOURCES
        } ?: emptySet()

    inline operator fun get(index: Int): KotON<Any> = koton[index]

    inline operator fun get(rel: String): KotON<Any> {
        return when (rel) {
            ACTIONS, RESOURCES -> throw IllegalArgumentException("There are no attribute with name '$rel'")
            else -> koton[rel]
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
        val links = koton[ACTIONS]
        val json = links[rel]
        return Link(json)
    }

    private fun Answer.execute(tail: (Answer.() -> Unit)? = null): Resource? {
        tail?.let {
            this.tail()
        }
        return this()?.let {
            Resource(it, aspect)
        } ?: null
    }

    fun FETCH(link: String? = null, vararg params: Pair<String, Any?>, headers: Headers? = null
              , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return FETCH(link, params.toMap(), headers, aspect, tail)
    }

    fun FETCH(link: String? = null, params: Map<String, Any?>, headers: Headers? = null
              , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link ?: "self").FETCH(params, headers = headers, aspect = aspect, handler = tail)
    }

    fun CREATE(link: String, vararg params: Pair<String, Any?>, body: String, headers: Headers? = null
               , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return CREATE(link, params.toMap(), body, headers, aspect, tail)
    }

    fun CREATE(link: String, params: Map<String, Any?>, body: String, headers: Headers? = null
               , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { POST(params, headers = headers, body = body) }
                .execute(tail)
                ?: Resource(kotON())
    }

    fun CREATE(link: String, params: Map<String, Any?>, source: BodySource, length: BodyLength, headers: Headers? = null
               , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { POST(params, headers = headers, source = source, length = length) }
                .execute(tail)
                ?: Resource(kotON())
    }

    fun REPLACE(link: String, vararg params: Pair<String, Any?>, body: String, headers: Headers? = null
                , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { PUT(*params, headers = headers, body = body) }
                .execute(tail)
                ?: Resource(kotON())
    }

    fun REPLACE(link: String, vararg params: Pair<String, Any?>, source: BodySource, length: BodyLength, headers: Headers? = null
                , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { PUT(*params, headers = headers, source = source, length = length) }
                .execute(tail)
                ?: Resource(kotON())
    }

    fun CREATE(link: String, vararg params: Pair<String, Any?>, file: File, headers: Headers? = null
               , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return CREATE(link, *params, files = mapOf(file.name to file), headers = headers, aspect = aspect, tail = tail)
    }

    fun CREATE(link: String, vararg params: Pair<String, Any?>, files: Collection<File>, headers: Headers? = null
               , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return CREATE(link, *params, files = files.map {
            it.name to it
        }.toMap(), headers = headers, aspect = aspect, tail = tail)
    }

    fun CREATE(link: String, vararg params: Pair<String, Any?>, files: Map<String, File>, headers: Headers? = null
               , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
//        return PLACE(link, params.toMap(), files, tail)
//    }
//
//    fun PLACE(link: String, params: Map<String, Any?>, files: Map<String, File>, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { UPLOAD(*params, headers = headers, files = files) }
                .execute(tail)
                ?: Resource(kotON())
    }

    fun UPDATE(link: String, params: Map<String, Any?> = emptyMap(), body: String, headers: Headers? = null
               , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { PATCH(headers = headers, body = body) }
                .execute(tail)
                ?: Resource(kotON())
    }

    fun FETCH(link: String, params: Map<String, Any?> = emptyMap(), folder: File, headers: Headers? = null
              , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { DOWNLOAD(headers = headers, file = folder) }
                .execute(tail)
                ?: Resource(kotON())
    }

    fun REMOVE(link: String, params: Map<String, Any?> = emptyMap(), headers: Headers? = null
               , aspect: (Link.(Link.() -> Answer) -> Answer) = this.aspect, tail: (Answer.() -> Unit)? = null): Resource {
        return getLink(link)
                .aspect { DELETE(headers = headers) }
                .execute(tail)
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
                    throw InvalidPropertiesFormatException(e)
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
        return try {
            body?.let {
                kotON(it)
            } ?: kotON { "" }
        } catch (e: InvalidPropertiesFormatException) {
            kotON(result.getAs<String>() ?: "")
        } // { null }
    }

    fun HttpStatus.getMessage(): String {
        return "$code ($reason)"
    }

    fun RERUN(vararg header: Pair<String, String>): Answer {
        val link = halSpeL(request.url.toExternalForm())
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

operator fun Headers.plus(header: Pair<String, String>): Headers {
    return append(header.first, header.second)
}

fun halSpeL(href: String, type: String? = null, templated: Boolean? = null): Link {
    return Link(
    kotON {
        "href" to href
        templated?.let {
            "templated" to it
        }
        "type" to (type ?: "application/hal+json")
    })
}
