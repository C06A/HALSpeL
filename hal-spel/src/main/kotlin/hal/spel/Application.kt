package hal.spel

import io.micronaut.runtime.Micronaut

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("hal.spel")
                .mainClass(Application.javaClass)
                .start()
    }
}