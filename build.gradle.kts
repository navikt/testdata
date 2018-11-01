import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val jacksonVersion = "2.9.6"
val jaxbApiVersion = "2.3.1"
val spekVersion = "2.0.0-rc.1"

plugins {
    java
    kotlin("jvm") version "1.3.0"
}

group = "no.nav.testdata"
version = "1.0-SNAPSHOT"

allprojects {
    group = "no.nav.testdata"
    version = "1.0-SNAPSHOT"

    repositories {
        jcenter()
        mavenCentral()
        maven {
            url = URI("https://repo.adeo.no/repository/maven-releases/")
        }
        maven {
            url = URI("https://repo.adeo.no/repository/maven-snapshots/")
        }
    }

    if (!name.endsWith("-js")) {
        apply(plugin = "kotlin")

        dependencies {
            implementation(kotlin("stdlib-jdk8"))
            implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
            implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
            implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
            implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
            implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

            implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")

            testImplementation("org.amshove.kluent:kluent:1.39")
            testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
            testRuntimeOnly("org.spekframework.spek2:spek-runtime-jvm:$spekVersion")
            testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging {
            showStandardStreams = true
        }
    }
}
