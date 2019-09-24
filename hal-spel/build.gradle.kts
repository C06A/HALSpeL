import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("io.spring.dependency-management") version "1.0.6.RELEASE"
    id("org.jetbrains.kotlin.jvm") version "1.3.21"
    id("org.jetbrains.kotlin.kapt") version "1.3.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.21"
    id("com.github.johnrengelman.shadow") version "4.0.2"
}

version = "0.2"
group = "hal.spel"

val kotlinVersion: String by project
val flue_version: String by project

repositories {
    mavenCentral()
    jcenter()
    maven("https://jcenter.bintray.com")
    maven("https://raw.githubusercontent.com/C06A/artifacts/libs-release")
}

dependencyManagement {
    imports {
        mavenBom("io.micronaut:micronaut-bom:1.1.1")
    }
}

configurations {
    // for dependencies that are needed for development only
//    developmentOnly {
//
//    }
}

dependencies {
    compile("io.micronaut:micronaut-management")
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    compile("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    compile("io.micronaut:micronaut-runtime")
    compile("io.micronaut.configuration:micronaut-micrometer-core")
    compile("io.micronaut:micronaut-http-client")
    compile("info.picocli:picocli")
    compile("io.micronaut.configuration:micronaut-picocli")
    compile("io.micronaut:micronaut-http-server-netty")
    kapt("io.micronaut:micronaut-inject-java")
    kapt("io.micronaut:micronaut-validation")
    kaptTest("io.micronaut:micronaut-inject-java")
    runtime("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
    runtime("ch.qos.logback:logback-classic:1.2.3")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.jetbrains.spek:spek-api:1.1.5")
    testCompile("io.micronaut.test:micronaut-test-junit5")
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
    testRuntime("org.jetbrains.spek:spek-junit-platform-engine:1.1.5")

    compile("com.github.kittinunf.fuel:fuel:$flue_version") //for JVM
    compile("com.github.kittinunf.fuel:fuel-gson:$flue_version") //for json support
    compile("com.github.kittinunf.fuel:fuel-jackson:$flue_version") //for json support

    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")

    implementation("com.google.code.gson:gson:2.8.5")

    compile("com.helpchoice.kotlin:koton:1.1.4")
}

application {
    mainClassName = "hal.spel.Application"
}

allOpen {
    annotation("io.micronaut.aop.Around")
}

val shadowJar: ShadowJar by tasks
shadowJar.mergeServiceFiles()

tasks.withType(KotlinCompile::class) {
    kotlinOptions {
        jvmTarget = "1.8"
//Will retain parameter names for Java reflection
        javaParameters = true
    }
}

val run: JavaExec by tasks
run.apply {
    //run.classpath += configurations.developmentOnly
    jvmArgs("-noverify", "-XX:TieredStopAtLevel=1", "-Dcom.sun.management.jmxremote")
}

//// use JUnit 5 platform
val test: Test by tasks
test.apply {
    useJUnitPlatform()
//test.classpath += configurations.developmentOnly
}
