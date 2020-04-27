package hal.spel

import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.*
import com.helpchoice.kotlin.koton.KotON
import io.micronaut.http.uri.UriTemplate
import java.io.File
import java.util.*

/**
 * This class provides low level access to HTTP protocol
 *
 */
data class Link(
        val href: String
        , val templated: Boolean? = null
        , val type: String? = null
        , val description: String? = null
        , val name: String? = null
        , val profile: String? = null
        , val title: String? = null
        , val hreflang: Locale? = null
        , val rel: String? = null
) {
    constructor(kotON: KotON<Any>, rel: String? = null) : this(
            kotON["href"]<String>() ?: throw Exception("The \"href\" field is required for Link.")
            , kotON<Boolean>("templated")
            , kotON<String>("type")
            , kotON<String>("description")
            , kotON<String>("name")
            , kotON<String>("profile")
            , kotON<String>("title")
            , kotON<Locale>("hreflang")
            , rel
    )

    private var parameters = mutableMapOf<String, Any?>()

    fun toMap(): Map<String, Any?> {
        return mapOf(
                "href" to href
                , "templated" to templated
                , "type" to type
                , "description" to description
                , "name" to name
                , "profile" to profile
                , "title" to title
                , "hreflang" to hreflang
        )
    }

    fun url(vararg params: Pair<String, Any?>): String {
        return url(mapOf(*params))
    }

    fun url(params: Map<String, Any?>?): String {
        return if (templated ?: false) {
            UriTemplate(href).expand(params)
        } else {
            href
        } ?: ""
    }

    infix fun String.conveys(value: Any?) {
        parameters[this] = value
    }

    fun GET(vararg params: Pair<String, Any?>, headers: Headers? = null): Answer {
        return GET(params.toMap(), headers)
    }

    fun GET(params: Map<String, Any?>?, headers: Headers?): Answer {
        return Answer(
                communicate(params, headers = headers) { httpGet() }
        )
    }

    fun POST(vararg params: Pair<String, Any?>, headers: Headers? = null, body: String): Answer {
        return POST(params.toMap(), headers, body)
    }

    fun POST(vararg params: Pair<String, Any?>, headers: Headers? = null, source: BodySource, length: BodyLength): Answer {
        return POST(params.toMap(), headers, source, length)
    }

    fun POST(params: Map<String, Any?>, headers: Headers? = null, body: String): Answer {
        return Answer(
                communicate(params, headers = headers) {
                    httpPost().run {
                        body(body)
                    }
                }
        )
    }

    fun POST(params: Map<String, Any?>, headers: Headers? = null, source: BodySource, length: BodyLength): Answer {
        return Answer(
                communicate(params, headers = headers) {
                    httpPost().body(source, length)
                }
        )
    }

    fun PUT(vararg params: Pair<String, Any?>, headers: Headers? = null, body: String): Answer {
        return Answer(
                communicate(*params, headers = headers) {
                    httpPut().body(body)
                }
        )
    }

    fun PUT(vararg params: Pair<String, Any?>, headers: Headers? = null, source: BodySource, length: BodyLength): Answer {
        return Answer(
                communicate(*params, headers = headers) {
                    httpPut().body(source, length)
                }
        )
    }

    fun PATCH(vararg params: Pair<String, Any?>, headers: Headers? = null, body: String): Answer {
        return Answer(
                communicate(*params, headers = headers) {
                    httpPatch().body(body)
                }
        )
    }

    fun PATCH(vararg params: Pair<String, Any?>, headers: Headers? = null, source: BodySource, length: BodyLength): Answer {
        return Answer(
                communicate(*params, headers = headers) {
                    httpPatch().body(source, length)
                }
        )
    }

    fun UPLOAD(vararg params: Pair<String, Any?>, headers: Headers? = null, files: Map<File, String>): Answer {
//        return UPLOAD(params.toMap(), headers, files)
//    }
//
//    fun UPLOAD(params: Map<String, Any?>, headers: Headers? = null, files: Map<String, File>): Answer {
        return Answer(
                communicate(*params, headers = headers) {
                    httpUpload()
                            .add(
                                    *(files.map { (key, value) ->
                                        FileDataPart(key, value)
                                    }).toTypedArray()
                            )
                }
        )
    }

    fun DOWNLOAD(vararg params: Pair<String, Any?>, headers: Headers? = null, file: File): Answer {
        return Answer(
                communicate(*params, headers = headers) {
                    httpDownload().fileDestination { _, _ -> file }
                }
        )
    }

    fun DELETE(vararg params: Pair<String, Any?>, headers: Headers? = null): Answer {
        return Answer(
                communicate(*params, headers = headers) { httpDelete() }
        )
    }

    private inline fun communicate(
            vararg params: Pair<String, Any?>
            , headers: Headers? = null
            , request: String.() -> Request): ResponseResultOf<String> {
        return communicate(params.toMap(), headers, request)
    }

    private inline fun communicate(
            params: Map<String, Any?>?
            , headers: Headers? = null
            , request: String.() -> Request): ResponseResultOf<String> {
        try {
            val updatedHeaders =
                    type?.let {
                        (headers ?: Headers()).run {
                            this + (!containsKey(Headers.ACCEPT)).OnlyIfTrue { Headers.ACCEPT to type }
                        }
                    } ?: headers
            return url(params)
                    .request()
                    .also {
                        it.executionOptions.allowRedirects = false
                        //                    it.headers.clear()
                        updatedHeaders?.apply {
                            it.headers.putAll(this)
                        }
                    }
//                .responseObject(jacksonDeserializerOf())
                    .responseString()
        } catch (e: Throwable) {
            println(e.localizedMessage)
            throw e
        }
    }
}

fun <R> Boolean.OnlyIfTrue(op: () -> R): R? {
    return if (this) {
        op()
    } else {
        null
    }
}