package hal.spel.rest

import com.github.kittinunf.fuel.core.Headers
import hal.spel.HalKt
import hal.spel.Link

class IpDashApi {
    static void main(String... args) {
        Link entrance = HalKt.halSpeL("http://ip-api.com{/format}{/query}{?fields,lang}"
                , null, true, "entry")

        Map<String, Object> entrancePlaceholders = [format: "json"]

        Headers jsonHeaders = new Headers()
        jsonHeaders += [Accept: "application/json"]

        entrance.GET(entrancePlaceholders, jsonHeaders).with {
            println("URL: " + request.url)
            println(status.code)
            println(body?.invoke())
            println(body?.get("query"))
        }

        List<String> fieldList = ["status", "query", "message", "continent", "continentCode", "country", "countryCode"
                                  , "region", "regionName", "city", "district", "zip", "lat", "lon", "timezone", "currency"
                                  , "isp", "org", "as", "asname", "reverse", "mobile", "proxy", "hosting"]

        entrancePlaceholders << [fields: fieldList]
        entrance.GET(entrancePlaceholders, jsonHeaders).with {
            println("URL: " + request.url)
            println(status.code)
            println(body?.invoke())
            println(body?.get("query"))
        }

        entrancePlaceholders.put("fields", fieldList.subList(1, 9))
        entrancePlaceholders.put("lang", "ru")
        entrance.GET(entrancePlaceholders, jsonHeaders).with {
            println("URL: " + request.url)
            println(status.code)
            println(body?.invoke())
            println(body?.get("query"))
        }

        entrancePlaceholders.clear()
        entrancePlaceholders.put("format", "json")
        entrancePlaceholders.put("query", "apple.com")
        entrance.GET(entrancePlaceholders, jsonHeaders).with {
            println("URL: " + request.url)
            println(status.code)
            println(body?.invoke())
            println(body?.get("query"))
        }

        entrancePlaceholders.put("query", "8.8.8.8")
        entrance.GET(entrancePlaceholders, jsonHeaders).with {
            println("URL: " + request.url)
            println(status.code)
            println(body?.invoke())
            println(body?.get("query"))
        }

        entrancePlaceholders.clear()
        entrancePlaceholders.put("format", "xml")
        entrance.GET(entrancePlaceholders, jsonHeaders).with {
            println("URL: " + request.url)
            println(status.code)
            println(result)
        }

        entrancePlaceholders.put("format", "csv")
        entrance.GET(entrancePlaceholders, jsonHeaders).with {
            println("URL: " + request.url)
            println(status.code)
            println(result)
        }
    }
}
