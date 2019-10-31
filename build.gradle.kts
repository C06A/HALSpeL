import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.50"
    kotlin("kapt") version "1.3.50"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "4.0.2"
    id("io.spring.dependency-management") version "1.0.6.RELEASE"
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
