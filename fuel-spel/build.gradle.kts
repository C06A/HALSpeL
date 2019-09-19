import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
}

version = "unspecified"

val sourceCompatibility = "1.8"

repositories {
    mavenCentral()
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
