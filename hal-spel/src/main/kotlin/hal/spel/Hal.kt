package hal.spel

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.*
import io.micronaut.http.HttpStatus
import java.io.File
import java.lang.IllegalArgumentException
import com.helpchoice.kotlin.koton.KotON
import com.helpchoice.kotlin.koton.kotON
import hal.spel.aspect.Aspect
import hal.spel.aspect.AspectFormatter
import hal.spel.aspect.makeDefaultAspectIfNull
import java.lang.Exception
import java.lang.NullPointerException
import java.util.*
import kotlin.math.max

const val ACTIONS = "_links"
const val RESOURCES = "_embedded"

private val SIMPLE_ASPECT: Link.(Link.() -> Answer) -> Answer = { it() }

/**
 * This method retrieves the Resource from the main entry point formed in Link.
 *
 * It expends the Link if it is a template, passes the function to submit request to the server to the aspect function,
 * passes the Answer it returns to the tail and returns the Resource resulting from all that.
 *
 * @param params -- key-value pairs to substitutes placeholders in the Link template
 * @param headers -- a collections of Headers to submit with the request
 * @param aspect -- the function to pass to all follow request to define pre- and post- processing
 * @param tail -- the post-processing for single request
 * @return the Resource returned by the server
 */
@JvmOverloads
fun Link.FETCH(
        vararg params: Pair<String, Any?>
        , headers: Headers? = null
        , aspect: Aspect = SIMPLE_ASPECT
        , tail: (Answer.() -> Unit)? = null
): Resource {
    return FETCH(params.toMap(), headers, aspect, tail)
}

/**
 * This method retrieves the Resource from the main entry point formed in Link.
 *
 * It expends the Link if it is a template, passes the function to submit request to the server to the aspect function,
 * passes the Answer it returns to the tail and returns the Resource resulting from all that.
 *
 * @param params -- key-value Map to substitutes placeholders in the Link template
 * @param headers -- a collections of Headers to submit with the request
 * @param aspect -- the function to pass to all follow request to define pre- and post- processing
 * @param tail -- the post-processing for single request
 * @return the Resource returned by the server
 */
@JvmOverloads
fun Link.FETCH(
        params: Map<String, Any?>
        , headers: Headers? = null
        , aspect: Aspect? = null
        , tail: (Answer.() -> Unit)? = null
): Resource {
    val answer = makeDefaultAspectIfNull(aspect).invoke(this) {
        GET(params, headers = headers)
    }
    tail?.let {
        answer.tail()
    }
    return answer().let {
        Resource(it, aspect)
    }
}

/**
 * This method submits the Resource to the main entry point formed in Link.
 *
 * It expends the Link if it is a template, passes the function to submit request to the server to the aspect function,
 * passes the Answer it returns to the handler and returns the Resource resulting from all that.
 *
 * @param params -- key-value pairs to substitutes placeholders in the Link template
 * @param headers -- a collections of Headers to submit with the request
 * @param body -- the definition of the new Resource to be created
 * @param aspect -- the function to pass to all follow request to define pre- and post- processing
 * @param tail -- the post-processing for single request
 * @return the Resource returned by the server
 */
@JvmOverloads
fun Link.CREATE(
        vararg params: Pair<String, Any?>
        , headers: Headers? = null
        , body: String = ""
        , aspect: Aspect = SIMPLE_ASPECT
        , tail: (Answer.() -> Unit)? = null
): Resource? {
    return CREATE(params.toMap(), headers, body, aspect, tail)
}

/**
 * This method submits the Resource to the main entry point formed in Link.
 *
 * It expends the Link if it is a template, passes the function to submit request to the server to the aspect function,
 * passes the Answer it returns to the handler and returns the Resource resulting from all that.
 *
 * @param params -- key-value pairs to substitutes placeholders in the Link template
 * @param headers -- a collections of Headers to submit with the request
 * @param body -- the definition of the new Resource to be created
 * @param aspect -- the function to pass to all follow request to define pre- and post- processing
 * @param tail -- the post-processing for single request
 * @return the Resource returned by the server
 */
@JvmOverloads
fun Link.CREATE(
        params: Map<String, Any?> = emptyMap()
        , headers: Headers? = null
        , body: String = ""
        , aspect: Aspect = SIMPLE_ASPECT
        , tail: (Answer.() -> Unit)? = null
): Resource? {
    val answer = aspect {
        POST(params, headers = headers, body = body)
    }
    tail?.let {
        answer.tail()
    }
    return Resource(answer(), aspect)
}

/**
 * This represents the server Resource referenced by REST endpoint.
 *
 * There is no need to create an instance of Resource, just use one returned by server.
 *
 * The Resource returns attribute value when addressed with square brackets.
 *
 * Calling Resource with label in parenthesis returns the referenced embedded Resource.
 */
class Resource(
        val koton: KotON<*>
        , var aspect: Aspect? = null
) {
    constructor(koton: KotON<*>, formatter: AspectFormatter) : this(koton, formatter.makeAspect())

    operator fun invoke(rel: String, index: Int? = null): Resource = Resource(
            index?.let {
                koton[RESOURCES, rel][index]
            } ?: koton[RESOURCES, rel]
            , aspect
    )

    fun embeddedList(rel: String): List<Resource>? {
        koton[RESOURCES, rel].let {
            return when (it.size()) {
                0 -> null
                1 -> listOf(Resource(it, aspect))
                else -> it<List<KotON<*>>>()?.map {
                    Resource(it, aspect)
                }
            }
        }
    }

    val references: Collection<String>
        inline get() = koton<Map<String, Any>>(ACTIONS)?.run {
            keys - "self"
        } ?: emptySet()

    val resources: Collection<String>
        inline get() = koton<Map<String, Any>>(RESOURCES)?.keys ?: emptySet()

    @Suppress("UNCHECKED_CAST")
    val attributes: Collection<String>
        inline get() = koton().run {
            when (this) {
                is Map<*, *> -> (this as Map<String, Any>).run {
                    keys - ACTIONS - RESOURCES
                }
                is Collection<*> -> this as Collection<String>
                else -> emptyList()
            }
        }

    operator fun get(index: Int): KotON<Any> = koton[index]

    operator fun get(rel: String, vararg path: String): KotON<Any> {
        return when (rel) {
            ACTIONS, RESOURCES -> throw IllegalArgumentException("There are no attribute with name '$rel'")
            else -> koton.get(rel, *path)
        }
    }

    /**
     * The String representation of the Resource includes the labels it contained grouped by References, Resources and Attributes.
     */
    override fun toString(): String {
        val refCount = references.fold(0) { count, key ->
            max(count, key.length)
        }
        val refs = references.map {
            val placeholders = koton[ACTIONS, it, "href"]<String>()
                    ?.split("{")
                    ?.drop(1)
                    ?.map {
                        it.split(Regex("}"), 2)[0]
                    }
            "${it.padEnd(refCount)} ${placeholders.nullIfEmpty()?.joinToString(", ", "[ ", " ]") ?: ""}"
        }

        val resCount = resources.fold(0) { count, key ->
            max(count, key.length)
        }
        val reses = resources.map {
            nullIfNeeded {
                val sub = koton[RESOURCES, it]()
                "${it.padEnd(resCount)} ${if (sub is Collection<*>) "[${sub.size}]" else ""}"
            }
        }.filterNotNull()

        val attCount = attributes.fold(0) { count, key ->
            max(count, key.length)
        }
        val atts = attributes.map {
            nullIfNeeded {
                "${it.padEnd(attCount)} ${koton[it].toJson()}"
            }
        }.filterNotNull()

        return """
            |${mutableListOf("References:").also { it.addAll(refs) }.joinToString("\n\t")}
            |${mutableListOf("Resources:").also { it.addAll(reses) }.joinToString("\n\t")}
            |${mutableListOf("Attributes:").also { it.addAll(atts) }.joinToString("\n\t")}
        """.trimMargin()
    }

    fun LINK(rel: String, index: Int? = null): Link {
        val links = koton[ACTIONS]
        val json = index?.let { links[rel][index] } ?: links[rel]
        return Link(json, rel)
    }

    private fun Answer.execute(tail: (Answer.() -> Unit)? = null): Resource? {
        tail?.let {
            this.tail()
        }
        return Resource(this(), aspect)
    }

    /**
     * This fetches the Resource from the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to retrieve the Resource from. No label will reload current Resource
     * @param index -- location of the link in the collection
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun FETCH(link: String? = null, index: Int
              , vararg params: Pair<String, Any?>
              , headers: Headers? = null
              , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return FETCH(link, index, params.toMap(), headers, aspect, tail)
    }

    /**
     * This fetches the Resource from the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to retrieve the Resource from. No label will reload current Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun FETCH(link: String? = null
              , vararg params: Pair<String, Any?>
              , headers: Headers? = null
              , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return FETCH(link, null, params.toMap(), headers, aspect, tail)
    }

    /**
     * This fetches the Resource from the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to retrieve the Resource from. No label will reload current Resource
     * @param params -- key-value Map to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun FETCH(link: String? = null, index: Int? = null
              , params: Map<String, Any?>
              , headers: Headers? = null
              , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return LINK(link ?: "self", index).FETCH(params, headers = headers, aspect = aspect, tail = tail)
    }

    /**
     * This submits the Resource to the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the definition of the new Resource to be created
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun CREATE(link: String? = null
               , vararg params: Pair<String, Any?>
               , body: KotON<*>, headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return CREATE(link, params.toMap(), body.toJson(), headers, aspect, tail)
    }

    /**
     * This submits the Resource to the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the definition of the new Resource to be created
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun CREATE(link: String? = null
               , vararg params: Pair<String, Any?>
               , body: String, headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return CREATE(link, params.toMap(), body, headers, aspect, tail)
    }

    /**
     * This submits the Resource to the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value Map to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the definition of the new Resource to be created
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun CREATE(link: String? = null
               , params: Map<String, Any?>
               , body: KotON<*>, headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return LINK(link ?: "self")
                .let {
                    makeDefaultAspectIfNull(aspect).invoke(it) {
                        POST(params, headers = headers, body = body.toJson())
                    }
                }
                .execute(tail)
                ?: Resource(kotON())
    }

    /**
     * This submits the Resource to the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value Map to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the definition of the new Resource to be created
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun CREATE(link: String? = null
               , params: Map<String, Any?>
               , body: String, headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return LINK(link ?: "self")
                .let {
                    makeDefaultAspectIfNull(aspect).invoke(it) {
                        POST(params, headers = headers, body = body)
                    }
                }
                .execute(tail)
                ?: Resource(kotON())
    }

    /**
     * This submits dynamically created Resource to the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value Map to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the function to dynamically create new Resources to pass to the server
     * @param length -- the function, providing the length of the created body
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun CREATE(link: String? = null
               , params: Map<String, Any?>
               , source: BodySource, length: BodyLength
               , headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return LINK(link ?: "self")
                .let {
                    makeDefaultAspectIfNull(aspect).invoke(it) {
                        POST(params, headers = headers, source = source, length = length)
                    }
                }
                .execute(tail)
                ?: Resource(kotON())
    }

    /**
     * This replaces the Resource on the server pointed by the reference in this Resource with new provided definition.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the definition of the new Resources to pass to the server
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun REPLACE(link: String? = null
                , vararg params: Pair<String, Any?>
                , body: KotON<*>, headers: Headers? = null
                , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return REPLACE(link, *params, body = body.toJson(), headers = headers, aspect = aspect, tail = tail)
    }

    /**
     * This replaces the Resource on the server pointed by the reference in this Resource with new provided definition.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the definition of the new Resources to pass to the server
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun REPLACE(link: String? = null
                , vararg params: Pair<String, Any?>
                , body: String, headers: Headers? = null
                , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return LINK(link ?: "self")
                .let {
                    makeDefaultAspectIfNull(aspect).invoke(it) {
                        PUT(*params, headers = headers, body = body)
                    }
                }
                .execute(tail)
                ?: Resource(kotON())
    }

    /**
     * This replaces the Resource on the server pointed by the reference in this Resource with new provided definition.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the function to dynamically create new Resources to pass to the server
     * @param length -- the function, providing the length of the created body
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun REPLACE(link: String? = null
                , vararg params: Pair<String, Any?>
                , source: BodySource, length: BodyLength
                , headers: Headers? = null
                , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return LINK(link ?: "self")
                .let {
                    makeDefaultAspectIfNull(aspect).invoke(it) {
                        PUT(*params, headers = headers, source = source, length = length)
                    }
                }
                .execute(tail)
                ?: Resource(kotON())
    }

    /**
     * This uploads the File to the server as new Resource pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param file -- the File definition to be uploaded
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun CREATE(link: String? = null
               , vararg params: Pair<String, Any?>
               , file: File, headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return CREATE(link, *params, files = mapOf(file to file.name), headers = headers, aspect = aspect, tail = tail)
    }

    /**
     * This uploads the Collection of Files to the server as new Resources pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param files -- a Collection of File definitions to be uploaded
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun CREATE(link: String? = null
               , vararg params: Pair<String, Any?>
               , files: Collection<File>
               , headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return CREATE(link, *params, files = files.map {
            it to it.name
        }.toMap(), headers = headers, aspect = aspect, tail = tail)
    }

    /**
     * This uploads the Collection of Files to the server as new Resources pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param files -- a Map of File name to File definition to be uploaded
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun CREATE(link: String? = null
               , vararg params: Pair<String, Any?>
               , files: Map<File, String>
               , headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
//        return PLACE(link, params.toMap(), files, tail)
//    }
//
//    fun PLACE(link: String, params: Map<String, Any?>, files: Map<String, File>, tail: (Answer.() -> Unit)? = null): Resource {
        return LINK(link ?: "self")
                .let {
                    makeDefaultAspectIfNull(aspect).invoke(it) {
                        UPLOAD(*params, headers = headers, files = files)
                    }
                }
                .execute(tail)
                ?: Resource(kotON())
    }

    /**
     * This updates provided fields in the Resource on the server pointed by the reference in this Resource.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param body -- the definition of fields in the Resources to update
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun UPDATE(link: String? = null
               , params: Map<String, Any?> = emptyMap()
               , body: KotON<*>, headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return LINK(link ?: "self")
                .let {
                    makeDefaultAspectIfNull(aspect).invoke(it) {
                        PATCH(*(params.toList().toTypedArray()), headers = headers, body = body.toJson())
                    }
                }
                .execute(tail)
                ?: Resource(kotON())
    }

    /**
     * This downloads the File from the server pointed by the reference in this Resource to the local folder.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param folder -- to download the File into
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun FETCH(link: String
              , vararg params: Pair<String, Any?>
              , folder: File
              , headers: Headers? = null
              , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        val linkObject = LINK(link)
        linkObject.let {
            makeDefaultAspectIfNull(aspect).invoke(it) {
                DOWNLOAD(*params, headers = headers, file = folder)
            }
        }.apply {
            tail?.let {
                this.it()
            }
        }
        return this
    }

    /**
     * This deletes the Resource pointed by the reference in this Resource from the server.
     *
     * @param link -- the label of the reference to point to the Resource
     * @param params -- key-value pairs to substitutes placeholders in the Link template
     * @param headers -- a collections of Headers to submit with the request
     * @param aspect -- the function to pass to all follow request to define pre- and post- processing
     * @param tail -- the post-processing for single request
     * @return the Resource returned by the server
     */
    @JvmOverloads
    fun REMOVE(link: String? = null
               , params: Map<String, Any?> = emptyMap()
               , headers: Headers? = null
               , aspect: Aspect? = this.aspect, tail: (Answer.() -> Unit)? = null
    ): Resource {
        return LINK(link ?: "self")
                .let {
                    makeDefaultAspectIfNull(aspect).invoke(it) {
                        DELETE(*(params.toList().toTypedArray()), headers = headers)
                    }
                }
                .execute(tail)
                ?: Resource(kotON())
    }
}

/**
 * This is a wrapper around connection resulting Object to simplify access to the Request, Responce and Result objects.
 */
class Answer(
        private val exchange: ResponseResultOf<String>
) {
    companion object {
        private val jackson = ObjectMapper()
    }

    val request
        get() = exchange.first

    val response
        get() = exchange.second

    val result
        get() = exchange.third

    val body: KotON<Any>?
        get() {
            val (success, failure) = result
            return kotON(
                    success?.let {
                        try {
                            jackson.readValue(it, Any::class.java)
                        } catch (e: Exception) {
                            throw InvalidPropertiesFormatException(e)
                        }
                    } ?: failure?.let {
                        if (response.headers["Content-Type"].any { it.contains("json") }) {
                            jackson.readValue(String(failure.errorData), Any::class.java)
                        } else {
                            failure.errorData
                        }
                    } ?: ""
            )
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
        return body ?: kotON { "" }
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

operator fun Headers.plus(header: Pair<String, String>?): Headers {
    return header?.let { append(header.first, header.second) } ?: this
}

fun halSpeL(href: String, type: String? = null, templated: Boolean? = null, rel: String? = null): Link {
    return Link(
            kotON {
                "href" to href
                templated?.let {
                    "templated" to it
                }
                "type" to (type ?: "application/hal+json")
            }, rel)
}

fun <V> nullIfNeeded(lambda: () -> V): V? {
    try {
        return lambda()
    } catch (e: NullPointerException) {
        return null
    }
}

fun Collection<*>?.nullIfEmpty() = if (this.isNullOrEmpty()) null else this
