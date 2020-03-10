package hal.spel.rest

import hal.spel.*
import io.micronaut.http.HttpStatus

fun main(vararg args: String) {
    val link = halSpeL("http://ip-api.com{/format}{/query}{?fields,lang}"
            , templated = true
    )

    val answer = link.GET("format" to "json")

    answer.apply {
        println("URL: ${request.url}")
        println(status.code)
        println(body?.invoke())
        println(body?.let { it["query"]() })
    }

    halSpeL("http://ip-api.com{/format}{/query}{?fields,lang}"
            , templated = true
    ).apply {
        val fieldList = listOf(
                "status"
                , "query"
                , "message"
                , "continent"
                , "continentCode"
                , "country"
                , "countryCode"
                , "region"
                , "regionName"
                , "city"
                , "district"
                , "zip"
                , "lat"
                , "lon"
                , "timezone"
                , "currency"
                , "isp"
                , "org"
                , "as"
                , "asname"
                , "reverse"
                , "mobile"
                , "proxy"
                , "hosting"
        )

        GET("format" to "json"
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println(body?.invoke())
            println(body?.let { it["query"]() })
        }

        GET("format" to "json"
                , "fields" to fieldList
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println(body?.invoke())
            println(body?.let { it["query"]() })
        }

        GET("format" to "json"
                , "query" to "apple.com"
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println(body?.invoke())
        }

        GET("format" to "json"
                , "query" to "8.8.8.8"
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println(body?.invoke())
        }

        GET("format" to "json"
                , "fields" to fieldList.subList(1, 9)
                , "lang" to "ru"
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println(body?.invoke())
            println(body?.let { it["query"]() })
        }

        GET("format" to "xml"
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println("body: ${result}")
        }

        GET("format" to "csv"
        ).apply {
            println("URL: ${request.url}")
            println(status.code)
            println("body: ${result}")
        }
    }
}
