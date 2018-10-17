package no.nav.datagen.norway

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.streams.toList

const val postenUrl = "https://www.bring.no/radgivning/sende-noe/adressetjenester/postnummer/_/attachment/download/7f0186f6-cf90-4657-8b5b-70707abeb789:24d75b0046859c6472007e4dbe23a7a4a246250d/Postnummerregister-ansi.txt"

val objectMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

fun generateNorData(baseDir: File) {
    val outputDirectory = baseDir.resolve("no")
    outputDirectory.mkdirs()
    generatePostalNumbers(outputDirectory)
}

fun generatePostalNumbers(outputDirectory: File) {
    val connection = URL(postenUrl).openConnection() as HttpURLConnection

    BufferedReader(InputStreamReader(connection.inputStream, Charsets.ISO_8859_1)).use {
        objectMapper.writeValue(outputDirectory.resolve("postalnumbers.yaml"), it.lines()
                .map { it.split("\t") }
                .map { PostalOffice(it[1], it[0].toInt()) }
                .toList())
    }

    connection.disconnect()

    //objectMapper.writeValue(outputDirectory.resolve("streets.yaml"), listOf("a", "b", "c"))
}

data class PostalOffice(
    val postalName: String,
    val postalNumber: Int
)
