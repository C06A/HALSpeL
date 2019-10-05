import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
//    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    id("org.jetbrains.kotlin.jvm") version "1.3.21"
    id("org.jetbrains.kotlin.kapt") version "1.3.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.21"
    id("com.github.johnrengelman.shadow") version "4.0.2"
}

version = "0.3"

val sourceCompatibility = "1.8"
val flue_version: String by project

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testCompile(group = "junit", name = "junit", version = "4.12")
}

tasks.withType(KotlinCompile::class) {
    kotlinOptions {
        jvmTarget = sourceCompatibility
        //Will retain parameter names for Java reflection
        javaParameters = true
    }
}
