package hal.spel.aspect

import hal.spel.Answer
import hal.spel.Link


typealias Aspect = Link.(Link.() -> Answer) -> Answer
typealias LinkFun = Link.() -> Unit
typealias AnswerFun = (Answer) -> Unit

fun makeDefaultAspectIfNull(aspect: Aspect?): Aspect {
    return aspect ?: { it() }
}

enum class CONN_PARTS {
    URL,
    LINK,
    CURL,
    HEADERS_OUT,
    COOKIES_OUT,
    BODY_OUT,
    STATUS,
    HEADERS_IN,
    COOKIES_IN,
    BODY_IN;

//    fun report(vararg logOn: CONN_PARTS, reporter: () -> Unit): Unit {
//        if (logOn.contains(this)) {
//            reporter()
//        }
//    }

}

fun makePreReporterAspect(vararg func: LinkFun, aspect: Aspect?): Aspect {
    return {
        func.forEach { reporter ->
            reporter()
        }
        makeDefaultAspectIfNull(aspect)(it)
    }
}

fun makePostReporterAspect(vararg func: AnswerFun?, aspect: Aspect?): Aspect {
    return {
        makeDefaultAspectIfNull(aspect)(it).apply {
            func.forEach { reporter ->
                reporter?.let {
                    it(this)
                }
            }
        }
    }
}
