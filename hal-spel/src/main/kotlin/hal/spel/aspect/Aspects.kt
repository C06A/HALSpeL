package hal.spel.aspect

import hal.spel.Answer
import hal.spel.Link


typealias Aspect = Link.(Link.() -> Answer) -> Answer
typealias LinkFun = Link.() -> Unit
typealias AnswerFun = (Answer) -> Unit

/**
 * Supplies the default Aspect if parameter is **null**
 */
fun makeDefaultAspectIfNull(aspect: Aspect?): Aspect {
    return aspect ?: { it() }
}

/**
 * Enlisting different parts of the Answer object to choose when reporting
 */
enum class PRE_PARTS {
    LINK,
    HREF,
    TYPE,
    TITLE,
    NAME,
    URL
}
enum class POST_PARTS {
    URL,
    CURL,
    HEADERS_OUT,
    COOKIES_OUT,
    BODY_OUT,
    STATUS,
    HEADERS_IN,
    COOKIES_IN,
    BODY_IN
}

/**
 * Generates Aspect to do something before sending request.
 *
 * This function creates the Aspect, which will call each **func** before passing control to the provided Aspect
 */
fun makePreFunctionAspect(vararg func: LinkFun, aspect: Aspect?): Aspect {
    return {
        func.forEach { reporter ->
            reporter()
        }
        makeDefaultAspectIfNull(aspect)(it)
    }
}

/**
 * Generates Aspect to do something after sending request.
 *
 * This function creates the Aspect, which will call each **func** after receives control from provided Aspect
 */
fun makePostFunctionAspect(vararg func: AnswerFun, aspect: Aspect?): Aspect {
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

/**
 * Generates Aspect to execute before sending request selected functions from the Map
 *
 * This method also accepts **Map<CONN_PARTS, (Link, (String) -> Unit) -> Unit>**, which
 * defines how to convert each part to the **String**.
 *
 * @param reporter -- the function to report the **String** representing the part
 * @param definitions -- the map to define String representation of each part
 * @param parts -- a list of parts to log
 * @param aspect -- the **Aspect** to chain from this or null if nothing
 */
fun makePreReporterAspect(reporter: (String) -> Unit
                          , definitions: Map<PRE_PARTS, (Link, (String) -> Unit) -> Unit>
                          , vararg parts: PRE_PARTS
                          , aspect: Aspect? = null): Aspect {
    return makePreFunctionAspect(
            *(parts.map {
                { link: Link ->
                    definitions[it]?.invoke(link, reporter) ?: Unit
                }
            }.toTypedArray())
            , aspect = makeDefaultAspectIfNull(aspect)
    )
}

/**
 * Generates Aspect to execute after sending request selected functions from the Map
 *
 * This method also accepts **Map<CONN_PARTS, (Link, (String) -> Unit) -> Unit>**, which
 * defines how to convert each part to the **String**.
 *
 * @param reporter -- the function to report the **String** representing the part
 * @param definitions -- the map to define String representation of each part
 * @param parts -- a list of parts to log
 * @param aspect -- the **Aspect** to chain from this or null if nothing
 */
fun makePostReporterAspect(reporter: (String) -> Unit
                           , definitions: Map<POST_PARTS, (Answer, (String) -> Unit) -> Unit>
                           , vararg parts: POST_PARTS
                           , aspect: Aspect? = null): Aspect {

    return makePostFunctionAspect(
            *(parts.map {
                { answer: Answer ->
                    definitions[it]?.invoke(answer, reporter) ?: Unit
                }
            }.toTypedArray())
            , aspect = makeDefaultAspectIfNull(aspect)
    )
}