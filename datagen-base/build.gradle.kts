import no.nav.datagen.DatagenTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
}

dependencies {
}

application {
    mainClassName = "no.nav.datagen.BootstrapKt"
}

sourceSets["main"].resources.srcDirs("$buildDir/generated-resources")

tasks {
    val generateData by tasks.creating(DatagenTask::class) {
        outputDirectory = file(project.buildDir).resolve("generated-resources")
    }

    "compileKotlin" {
        dependsOn(generateData)
    }
}
