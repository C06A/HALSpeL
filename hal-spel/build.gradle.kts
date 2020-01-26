import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val dokka_version = "0.10.0"

    application
    maven
    kotlin("jvm")
    kotlin("kapt")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("com.github.johnrengelman.shadow")
    id("org.jetbrains.dokka") version dokka_version
}

version = "1.3.1"
group = "hal.spel"

val kotlinVersion: String by project
val flue_version: String by project

repositories {
    mavenCentral()
    jcenter()
}

dependencyManagement {
    imports {
        mavenBom("io.micronaut:micronaut-bom:1.2.2")
    }
}

val developmentOnly by configurations.creating
configurations {
    // for dependencies that are needed for development only
    developmentOnly
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
    compile("com.github.kittinunf.fuel:fuel-jackson:$flue_version") //for json support

    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")

    implementation("com.google.code.gson:gson:2.8.5")

    compile("com.helpchoice.kotlin:koton:1.1.6")
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
    classpath += developmentOnly
}

val sourcesJar = task<Jar>("sourcesJar") {
    group = "build"

    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

task("writeNewPom") {
    group = "build"

    doLast {
        maven.pom {
            withGroovyBuilder {
                "project" {
                    setProperty("inceptionYear", "2019")
                    "licenses" {
                        "license" {
                            setProperty("name", "The Apache Software License, Version 2.0")
                            setProperty("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")
                            setProperty("distribution", "repo")
                        }
                    }
                }
            }
        }.writeTo("$buildDir/libs/${project.name}-${version}.pom")
    }
}

val dokka: DokkaTask by tasks
dokka.apply {
    outputFormat = "html"
    outputDirectory = "$buildDir/dokka"
}

val dokkadocJar = task<Jar>("dokkadocJar") {
    dependsOn(dokka)
    group = "build"

    classifier = "javadoc"
    from(dokka.outputDirectory)
}
