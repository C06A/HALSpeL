import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.jetbrains.kotlin.jvm")
}

version = "unspecified"

val sourceCompatibility = "1.8"
val flue_version: String by project

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("com.github.kittinunf.fuel:fuel:$flue_version") //for JVM
    testCompile(group = "junit", name = "junit", version = "4.12")
}

tasks.withType(KotlinCompile::class) {
    kotlinOptions {
        jvmTarget = sourceCompatibility
        //Will retain parameter names for Java reflection
        javaParameters = true
    }
}
