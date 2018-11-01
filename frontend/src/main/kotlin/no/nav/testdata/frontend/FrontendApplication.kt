package no.nav.testdata.frontend

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.html.HtmlBlockTag
import kotlinx.html.InputType
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.meta
import kotlinx.html.option
import kotlinx.html.select
import kotlinx.html.style
import kotlinx.html.textInput
import no.nav.datagen.GDoctor
import no.nav.datagen.GPerson
import no.nav.helse.sm2013.ArsakType
import no.nav.helse.sm2013.CS
import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.helse.sm2013.TeleCom
import no.nav.testdata.helse.GHealthReason
import no.nav.testdata.helse.GPersonIdent
import no.nav.testdata.helse.GPhoneType
import no.nav.testdata.helse.GWorkplaceReason
import no.nav.testdata.helse.ICD10
import no.nav.testdata.helse.ICPC2
import no.nav.testdata.helse.Kodeverk
import no.nav.testdata.helse.OtherAbsenceReason
import no.nav.testdata.helse.toSM2013
import java.util.UUID
import javax.xml.datatype.XMLGregorianCalendar
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible

val diagnoseKodeMapper = { html: HtmlBlockTag, objVal: Any?, _: List<String> ->
    html.select {
        val cv = objVal as CV?
        ICPC2.values().forEach {
            //println("${cv.v == it.codeValue} ${cv.v} == ${it.codeValue}")
            option {
                value = it.codeValue
                selected = cv?.v == it.codeValue
                +"ICPC2 (${it.codeValue}) ${it.text}"
            }
        }
        ICD10.values().forEach {
            //println("${cv.v == it.codeValue} ${cv.v} == ${it.codeValue}")
            option {
                value = it.codeValue
                selected = cv?.v == it.codeValue
                +"ICD10 (${it.codeValue}) ${it.text}"
            }
        }
    }
}

val csListMappings = mapOf<String, List<Kodeverk>>(
        "annenFraversArsak" to OtherAbsenceReason.values().toList(),
        "medisinskeArsaker" to GHealthReason.values().toList(),
        "arbeidsplassen" to GWorkplaceReason.values().toList()
)

val listTypeMapping = mapOf<(path: List<String>) -> Boolean, KClass<*>>(
        { path: List<String> -> "diagnosekode" in path } to HelseOpplysningerArbeidsuforhet.MedisinskVurdering.BiDiagnoser::class,
        { path: List<String> -> "ident" in path } to CV::class,
        { path: List<String> -> "arsakskode" in path } to CS::class
)

val fieldMapping: Map<KClass<*>, (html: HtmlBlockTag, objVal: Any?, path: List<String>) -> Unit> = mapOf(
        HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose::class to diagnoseKodeMapper,
        HelseOpplysningerArbeidsuforhet.MedisinskVurdering.BiDiagnoser::class to diagnoseKodeMapper,
        Ident::class to { html, objVal, _ ->
            val cv = objVal as CV?
            html.div {
                select {
                    option {}
                    GPersonIdent.values().forEach {
                        option {
                            value = it.codeValue
                            selected = cv?.v == it.codeValue
                            +"(${it.codeValue}) ${it.text}"
                        }
                    }
                }
            }
        },
        ArsakType::class to { html, objVal, path ->
            val cs = objVal as CS?
            html.select {
                option {}
                csListMappings.entries.find { it.key in path }!!.value.forEach {
                    option {
                        value = it.codeValue
                        selected = cs?.v == it.codeValue
                        +"(${it.codeValue}) ${it.text}"
                    }
                }
            }
        },
        TeleCom::class to { html, objVal, _ ->
            val cs = objVal as CS?
            html.select {
                option {}
                GPhoneType.values().forEach {
                    option {
                        value = it.codeValue
                        selected = cs?.v == it.codeValue
                        +"(${it.codeValue}) ${it.text}"
                    }
                }
            }
        }
)

fun main(args: Array<String>) {
    embeddedServer(CIO, 8080) {
        routing {
            get("/") {
                call.respondHtml {
                    head {
                        meta {
                            charset = "UTF-8"
                        }
                    }
                    body {
                        form {
                            build(GPerson().toSM2013(GPerson(extensions = listOf(GDoctor()))), path = listOf("Sykmelding"))
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}

fun HtmlBlockTag.build(input: Any?, type: KClass<out Any> = input!!::class, path: List<String>) {
    div(classes = "${type.simpleName}") {
        style = "border: 1px solid black; margin: 5px 20px"
        h1 {
            +"${path.last()}(${type.simpleName})"
        }
        for (obj in type.declaredMemberProperties) {
            obj.isAccessible = true
            val cl = obj.returnType.classifier as KClass<*>
            val objVal = if (input == null) {
                null
            } else {
                obj.getter.call(input)
            }
            createFieldForType(objVal, cl, type, listOf(path, listOf(obj.name)).flatten())
        }
        br {  }
    }
}

fun HtmlBlockTag.mapEmptyList(parent: KClass<*>, path: List<String>) = listTypeMapping.entries.find { it.key(path) }?.let {
    createFieldForType(null, it.value, parent, path, true)
} ?: println("Unknown list entry mapping with path $path")

fun HtmlBlockTag.createFieldForType(objVal: Any?, cl: KClass<*>, parent: KClass<*>, path: List<String>, isList: Boolean = false) {
    val inputId = UUID.randomUUID().toString()
    when {
        (cl.isSubclassOf(CS::class) || cl.isSubclassOf(CV::class)) && fieldMapping.containsKey(parent) -> fieldMapping[parent]!!(this, objVal, path)
        cl.isSubclassOf(String::class) -> div {
            label {
                htmlFor = inputId
                +path.last()
            }
            textInput {
                id = inputId
                value = objVal as String? ?: ""
            }
        }
        cl.isSubclassOf(Int::class) -> input(type = InputType.number) { value = objVal.toString() }
        cl.isSubclassOf(Iterable::class) -> div(classes = "list_type") {
            if (objVal == null) {
                mapEmptyList(parent, path)
            } else {
                val count = (objVal as Iterable<*>).count {
                    createFieldForType(it, it!!::class, parent, path, true)
                    true
                }
                if (count == 0) {
                    mapEmptyList(parent, path)
                }
            }
            input(type = InputType.button) {
                value = "+"
            }
        }
        cl.isSubclassOf(XMLGregorianCalendar::class) -> div {
            label {
                htmlFor = inputId
                +path.last()
            }
            input(type = InputType.dateTime) {
                id = inputId
                value = (objVal as XMLGregorianCalendar?)?.toGregorianCalendar()?.toZonedDateTime().toString()
            }
        }
        cl.isSubclassOf(Boolean::class) -> div {
            label {
                htmlFor = inputId
                +path.last()
            }
            input(type = InputType.checkBox) {
                id = inputId
                checked = objVal as Boolean? ?: false
            }
        }
        cl.isSubclassOf(CS::class) -> div {
            div {
                label {
                    htmlFor = inputId
                    +path.last()
                }
                input(type = InputType.text) {
                    id = inputId
                    value = (objVal as CS?)?.v ?: ""
                }
            }
        }
        cl.isSubclassOf(CV::class) -> div {
            div {
                val cv = (objVal as CV?)
                label {
                    htmlFor = inputId
                    +"${path.last()}(${cv?.s})"
                }
                input(type = InputType.text) {
                    id = inputId
                    value = cv?.v ?: ""
                }
            }
        }
        cl.qualifiedName?.startsWith("kotlin.") ?: false -> println("Support for $cl not implemented")
        objVal != null -> build(objVal, cl, path)
        else -> build(objVal, cl, path)
        //else -> println("Unmapped list entry with class $cl found")
    }
}
