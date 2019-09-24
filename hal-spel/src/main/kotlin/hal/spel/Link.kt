package hal.spel

import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.*
import com.helpchoice.kotlin.koton.KotON
import io.micronaut.http.uri.UriTemplate
import java.io.File

data class Link(val kotON: KotON<Any>?) {

    val href: String? = kotON?.let {
        it<String>("href")
    }

    val type: String? = kotON?.let {
        it<String>("type")
    }

    val templated: Boolean = kotON?.let {
        it<Boolean>("templated")
    } ?: false

    val name: String? = kotON?.let {
        it<String>("name")
    }

    val description: String? = kotON?.let {
        it<String>("description")
    }

    private var parameters = mutableMapOf<String, Any?>()

    fun url(vararg params: Pair<String, Any?>): String {
        return url(mapOf(*params))
    }

    fun url(params: Map<String, Any?>): String {
        return if (templated) {
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

    fun GET(params: Map<String, Any?>, headers: Headers? = null): Answer {
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
                    httpPost().body(body)
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

    fun UPLOAD(vararg params: Pair<String, Any?>, headers: Headers? = null, files: Map<String, File>): Answer {
//        return UPLOAD(params.toMap(), headers, files)
//    }
//
//    fun UPLOAD(params: Map<String, Any?>, headers: Headers? = null, files: Map<String, File>): Answer {
        return Answer(
                communicate(*params, headers = headers) {
                    httpUpload()
                            .add(
                                    *(files.map { (key, value) ->
                                        FileDataPart(value, key, value.name)
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
            params: Map<String, Any?>
            , headers: Headers? = null
            , request: String.() -> Request): ResponseResultOf<String> {
        try {
            return url(params)
                    .request()
                    .also {
                        it.executionOptions.allowRedirects = false
                        //                    it.headers.clear()
                        headers?.apply {
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
