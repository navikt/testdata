val kotlinxHtmlVersion = "0.6.11"
val ktorVersion = "1.0.0-beta-3"
val logbackVersion = "1.3.0-alpha4"

repositories {
    maven { url = uri("https://dl.bintray.com/kotlin/ktor/") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
    implementation(project(":sm2013-support"))

    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}
