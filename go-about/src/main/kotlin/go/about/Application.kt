package go.about

import io.micronaut.runtime.Micronaut

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("go.about")
                .mainClass(Application.javaClass)
                .start()
    }
}