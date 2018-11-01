plugins {
    id("kotlin2js") version "1.3.0-rc-190"
}

group = "no.nav.testdata"
version = "1.0-SNAPSHOT"

repositories {
    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-js"))
}
