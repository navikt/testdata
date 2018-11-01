val kotlinxHtmlVersion = "0.6.11"

plugins {
    id("kotlin2js")
}

group = "no.nav.testdata"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-js"))
    compile("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinxHtmlVersion")
}
