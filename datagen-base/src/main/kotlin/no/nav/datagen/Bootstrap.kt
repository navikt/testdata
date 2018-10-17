package no.nav.datagen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

val objectMapper: ObjectMapper = ObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(JavaTimeModule())

fun main(args: Array<String>) {
    val person = GPerson()
    println(objectMapper.writeValueAsString(person))
}
