package no.nav.datagen

import no.nav.datagen.norway.generateNorData
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class DatagenTask : DefaultTask() {
    @OutputDirectory
    lateinit var outputDirectory: File

    @TaskAction
    fun generateData() {
        generateNorData(outputDirectory)
    }
}
