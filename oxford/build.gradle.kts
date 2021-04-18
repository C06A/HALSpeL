import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.internal.JavaJarExec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.asciidoctor.gradle.jvm.AsciidoctorTask

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
    groovy
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("com.github.johnrengelman.shadow")
    id("org.asciidoctor.jvm.base") version "3.2.0"
    id("org.asciidoctor.jvm.convert") version "3.2.0"
    id("org.asciidoctor.jvm.pdf") version "3.2.0"
    id("org.asciidoctor.jvm.epub") version "3.2.0"
}

version = "1.5.4"
group = "oxford"

val kotlinVersion: String by project
val micronautVersion: String by project
val flue_version: String by project

repositories {
    mavenCentral()
    jcenter()
}

configurations {
    // for dependencies that are needed for development only
//    developmentOnly {
//
//    }
}

dependencies {
    implementation(platform("io.micronaut:micronaut-bom:$micronautVersion"))
    compile("io.micronaut:micronaut-management")
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    compile("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    compile("io.micronaut:micronaut-runtime")
    compile("io.micronaut.configuration:micronaut-micrometer-core")
    compile("io.micronaut:micronaut-http-client")
    compile("info.picocli:picocli")
    compile("io.micronaut.configuration:micronaut-picocli")
    compile("io.micronaut:micronaut-http-server-netty")
    compile("org.slf4j:slf4j-api:1.7.25")

    compile("com.helpchoice.kotlin:koton:1.1.6")

    compile(project(":fuel-spel"))
//    compile(project(":hal-spel"))
    compile("hal.spel:hal-spel:1.7.3")

    compileOnly("io.micronaut:micronaut-inject-groovy")
    implementation("io.micronaut:micronaut-runtime-groovy")

    kapt(platform("io.micronaut:micronaut-bom:$micronautVersion"))
    kapt("io.micronaut:micronaut-inject-java")
    kapt("io.micronaut:micronaut-validation")
    kaptTest("io.micronaut:micronaut-inject-java")

    implementation("com.google.code.gson:gson:2.8.5")

    runtime("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
    runtime("ch.qos.logback:logback-classic:1.2.3")

    testAnnotationProcessor("io.micronaut:micronaut-inject-java")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.jetbrains.spek:spek-api:1.1.5")
    testCompile("io.micronaut.test:micronaut-test-junit5")
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
    testRuntime("org.jetbrains.spek:spek-junit-platform-engine:1.1.5")

    testImplementation("io.micronaut:micronaut-inject-groovy")
    testImplementation("org.spockframework:spock-core") {
        exclude("org.codehaus.groovy:groovy-all")
    }
}

application {
    mainClassName = "oxford.Oxford"
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
    workingDir = rootDir
    jvmArgs("-noverify", "-XX:TieredStopAtLevel=1", "-Dcom.sun.management.jmxremote")
}

val runShadow: JavaJarExec by tasks
runShadow.apply {
    workingDir = rootDir
}

//// use JUnit 5 platform
val test: Test by tasks
test.apply {
    useJUnitPlatform()
//test.classpath += configurations.developmentOnly
}

val asciidoctor: AsciidoctorTask by tasks
asciidoctor.apply {
    baseDirFollowsSourceFile()
    logDocuments = true
    sourceDir("src/main/asciidoc")

    outputOptions {
        backends("html5", "pdf")
    }
}
