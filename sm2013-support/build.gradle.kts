import no.nav.codegen.CodegenTask

val sm2013XMLVersion = "1.0-SNAPSHOT"

val jacksonVersion = "2.9.6"

val jaxbApiVersion = "2.1"
val jaxbVersion = "2.3.0.1"

dependencies {
    implementation(project(":datagen-base"))
    implementation("no.nav.helse.xml:sm2013:$sm2013XMLVersion")

    implementation("com.sun.xml.bind:jaxb-impl:$jaxbVersion")
    implementation("com.sun.xml.bind:jaxb-core:$jaxbVersion")
    implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")
}

sourceSets["main"].java.srcDirs("$buildDir/generated-sources")

tasks {
    val generateDiagnoseCodes by tasks.creating(CodegenTask::class) {
        outputDirectory = file(project.buildDir).resolve("generated-sources")
    }

    "compileKotlin" {
        dependsOn(generateDiagnoseCodes)
    }
}
