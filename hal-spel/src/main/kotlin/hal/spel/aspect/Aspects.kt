package hal.spel.aspect

import hal.spel.Answer
import hal.spel.Link


typealias Aspect = Link.(Link.() -> Answer) -> Answer
typealias LinkFun = Link.() -> Link
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

enum class ReportPart {
    REL,
    LINK,
    URI,
    URL,
    TYPE,
    TITLE,
    NAME,
    HEADERS_OUT,
    COOKIES_OUT,
    BODY_OUT,
    CURL,
    STATUS,
    HEADERS_IN,
    COOKIES_IN,
    BODY_IN
}

@Deprecated("Temporary aliases for backward compatibility. Use `ReportPart` instead")
typealias PRE_PARTS = ReportPart
@Deprecated("Temporary aliases for backward compatibility. Use `ReportPart` instead")
typealias POST_PARTS = ReportPart

/**
 * Generates Aspect to do something before sending request.
 *
 * This function creates the Aspect, which will call each **func** before passing control to the provided Aspect
 */
fun makePreFunctionAspect(vararg func: LinkFun, aspect: Aspect?): Aspect {
    return {
        func.fold(this) { link, reporter ->
            link.reporter()
        }.(makeDefaultAspectIfNull(aspect))(it)
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
                reporter(this)
            }
        }
    }
}

/**
 * Generates Aspect to do sequence of functions before and after sending request.
 *
 * This is most generic aspect factory method. It will make follow calles:
 *
 * #. Call first functions from each pair in order
 * #. Call provided aspect or default one if nothing provided
 * #. Call second functions from each pair in reverse order
 *
 * This factory allows to build complex aspect by accepting the collection of Pre- and Post-
 * functions. This simplifies creation of the aspect from maps or enum collections.
 */
fun makeFunctionAspect(vararg func: Pair<LinkFun?, AnswerFun?>?, aspect: Aspect?): Aspect {
    return {
        func.map { pair ->
            pair?.first
        }.filterIsInstance<LinkFun>().fold(this) { link, reporter ->
            link.reporter()
        }.(makeDefaultAspectIfNull(aspect))(it).apply {
            func.map {pair ->
                pair?.second }.filterIsInstance<AnswerFun>().forEach { reporter ->
                reporter(this)
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
                          , definitions: Map<ReportPart, (Link, (String) -> Unit) -> Link>
                          , vararg parts: ReportPart
                          , aspect: Aspect? = null): Aspect {
    return makePreFunctionAspect(
            *(parts.map {
                { link: Link ->
                    definitions[it]?.invoke(link, reporter) ?: link
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
                           , definitions: Map<ReportPart, (Answer, (String) -> Unit) -> Unit>
                           , vararg parts: ReportPart
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

/**
 * Generates Aspect to execute before and after sending request selected functions from the Map
 *
 * This method also accepts **Map<CONN_PARTS, (Link, (String) -> Unit) -> Unit>**, which
 * defines how to convert each part to the **String**.
 *
 * @param definitions -- the map to define String representation of each part
 * @param parts -- a list of parts to log
 * @param aspect -- the **Aspect** to chain from this or null if nothing
 */
fun makeReporterAspect(definitions: Map<ReportPart, Pair<LinkFun?, AnswerFun?>>
                       , vararg parts: ReportPart
                       , aspect: Aspect? = null): Aspect {

    return makeFunctionAspect(
            *(parts.map {
                definitions[it]
            }.toTypedArray())
            , aspect = makeDefaultAspectIfNull(aspect)
    )
}

//open class AspectMaker(private val preFuncs: List<LinkAspect> = emptyList(), private val postFuncs: List<AnswerAspect> = emptyList()) {
//    var pre: LinkFun = {}
//        set(value) {
//            preFuncs + value
//        }
//
//    var post: AnswerFun = {}
//        set(value) {
//            postFuncs + value
//        }
//
//    fun makeAspect(aspect: Aspect?): Aspect {
//        return {
//            preFuncs.fold(this) { acc: Link, current: LinkAspect ->
//                acc.current()
//            }.run {
//                postFuncs.foldRight(makeDefaultAspectIfNull(aspect)(it)) { current, acc ->
//                    current(acc)
//                }
//            }
//        }
//    }
//}

/**
 * This abstract class can be used to build different aspects to format request/response into different targets.
 */
abstract class AspectFormatter(private val followed: AspectFormatter? = null) {
    /**
     * This function builds an aspect function, which wraps around provided aspect.
     * <p/>
     * The child class should override this function to format Link/Answer on its own way and should call this
     * function to wrap around parameter.
     */
    open fun makeAspect(aspect: Aspect? = null): Aspect {
        return makeDefaultAspectIfNull(followed?.makeAspect(aspect) ?: aspect)
    }
}
