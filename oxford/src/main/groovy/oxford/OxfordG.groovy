package oxford

import hal.spel.Answer
import hal.spel.HalKt
import hal.spel.Link
import hal.spel.Resource
import hal.spel.aspect.ADocTagAspectKt
import hal.spel.aspect.POST_PARTS
import hal.spel.aspect.PRE_PARTS
import io.micronaut.http.MediaType
import kotlin.Unit
import kotlin.jvm.functions.Function1
import kotlin.jvm.functions.Function2

class OxfordG {
    static String entry = "http://api.m.ox.ac.uk"

    static Function2<? super Link, ? super Function1<? super Link,Answer>, Answer> aspect = { link, aspect ->
        Link newLink = new Link(entry + link.href
                , link.templated, link.type
                , link.getDescription(), link.getName(), link.getProfile()
                , link.getTitle(), link.getHreflang(), link.getRel())
        return aspect.invoke(newLink)
    }

    static Function1<String, Unit> reporter = { String it ->
        System.out.println()
        System.out.println(it)
        return null as Unit
    }

    static void main(String... args) {
        PRE_PARTS[] preParts = [ PRE_PARTS.REL, PRE_PARTS.LINK ]

        Link entrance = HalKt.halSpeL("/"
                , MediaType.APPLICATION_HAL_JSON
                , false, "entryPoint"
        )
        Resource resource = HalKt.FETCH(entrance, Collections.emptyMap(), null
                , ADocTagAspectKt.makePostADocTagAspect(reporter
                        , POST_PARTS.values()
                        , ADocTagAspectKt.makePreADocTagAspect(reporter, preParts, aspect))
                , null
        )

        resource.FETCH("app:contacts")

        Resource resource1 = resource.FETCH("app:courses")

        resource1.FETCH("hl:course", Collections.singletonMap("id", 10))

        Resource resource2 = resource1.FETCH("hl:subjects")
        resource2.FETCH("courses:subject", 0)

        resource1.FETCH("hl:search", Collections.singletonMap("q","Russian"))

        resource.FETCH("app:library")
    }
}
