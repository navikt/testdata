package no.nav.codegen

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

data class Entry(
    val icpc2CodeValue: String,
    val icpc2FullText: String,
    val icd10CodeValue: String,
    val icd10Text: String
) {
    val icpc2EnumName = icpc2CodeValue.replace("-", "NEGATIVE_")
}

fun generateDiagnoseCodes(outputDirectory: Path) {
    val connection = URL("https://ehelse.no/Documents/Helsefaglig%20kodeverk/ICPC-2%20til%20ICD-10%20konverteringsfil%20%28txt%29.TXT").openConnection() as HttpURLConnection

    val entries = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
        reader.readLines()
                .filter { !it.matches(Regex(".?--.+")) }
                .map { CSVParser.parse(it, CSVFormat.DEFAULT.withDelimiter(';')) }.flatMap { it.records }
                .map {
                    Entry (icpc2CodeValue = it[0], icpc2FullText = it[1], icd10CodeValue = it[3], icd10Text = it[4])
                }
    }

    Files.newBufferedWriter(outputDirectory.resolve("ICPC2.kt")).use { writer ->
        writer.write("package no.nav.testdata.helse\n\n")
        writer.write("enum class ICPC2(override val codeValue: String, override val text: String, val icd10: List<ICD10>, override val oid: String = \"2.16.578.1.12.4.1.1.7170\") : Kodeverk {\n")
        entries
                .groupBy { it.icpc2CodeValue }
                .map {
                    (_, entries) ->
                    val firstEntry = entries.first()
                    "    ${firstEntry.icpc2EnumName}(\"${firstEntry.icpc2CodeValue}\", \"${firstEntry.icpc2FullText}\", ${entries.joinToString(", ", "listOf(", ")") { "ICD10.${it.icd10CodeValue}" }}),\n"
                }
                .forEach { writer.write(it) }
        writer.write("}\n")
    }

    Files.newBufferedWriter(outputDirectory.resolve("ICD10.kt")).use { writer ->
        writer.write("package no.nav.testdata.helse\n\n")
        writer.write("enum class ICD10(override val codeValue: String, override val text: String, val icpc2: List<ICPC2>, override val oid: String = \"2.16.578.1.12.4.1.1.7110\") : Kodeverk {\n")

        entries.groupBy { it.icd10CodeValue }
                .map { (_, entries) ->
                    val firstEntry = entries.first()
                    "    ${firstEntry.icd10CodeValue}(\"${firstEntry.icd10CodeValue}\", \"${firstEntry.icd10Text}\", ${entries.joinToString(", ", "listOf(", ")") { "ICPC2.${it.icpc2EnumName}" }}),\n"
                }
                .forEach { writer.write(it) }
        writer.write("}\n")
        writer.close()
    }

    connection.disconnect()
}
