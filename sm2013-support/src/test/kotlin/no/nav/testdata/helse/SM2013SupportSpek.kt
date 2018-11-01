package no.nav.testdata.helse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.datagen.GDoctor
import no.nav.datagen.GPerson
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

object SM2013SupportSpek : Spek({
    val xmlMapper: ObjectMapper = XmlMapper(JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    }).registerModule(JaxbAnnotationModule())
            .registerKotlinModule()

    val marshaller: Marshaller = JAXBContext.newInstance(HelseOpplysningerArbeidsuforhet::class.java).createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

    fun Marshaller.toString(input: Any): String = StringWriter().use {
        marshal(input, it)
        it.toString()
    }

    describe("Random generated SM2013 should create XML") {
        it("Generates a XML") {
            println(marshaller.toString(GPerson().toSM2013(GPerson(extensions = listOf(GDoctor())))))
        }
        it("Generates 10 xml files") {
            for (i in 1..10) {
                Files.write(Paths.get("build/test-results/sykmelding-$i.xml"), marshaller.toString(GPerson().toSM2013(GPerson(extensions = listOf(GDoctor())))).toByteArray(Charsets.UTF_8))
            }
        }
    }
})
