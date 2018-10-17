package no.nav.datagen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Random

fun Random.nextLong(min: Long, max: Long): Long =
        min + (nextDouble() * (max - min)).toLong()
fun Random.nextInt(min: Int, max: Int): Int =
        min + (nextDouble() * (max - min)).toInt()

fun <T> Random.nextEntry(list: List<T>): T = list[nextInt(list.size)]

fun Random.dateBetween(start: ZonedDateTime, end: ZonedDateTime): ZonedDateTime =
        Instant.ofEpochSecond(nextLong(start.toEpochSecond(), end.toEpochSecond())).atZone(ZoneId.systemDefault())

fun <T> Random.withChance(chance: Int, range: Int, result: () -> T): T? = if (nextInt(range / chance) == 0) {
    result()
} else {
    null
}

fun Random.numberWithFormat(format: String) = format.toCharArray().map {
    if (it == '#') {
        Integer.toString(nextInt(10))[0]
    } else {
        it
    }
}.joinToString("")

class Generator(
    seed: Long = System.currentTimeMillis(),
    val maxAge: Long = 100,
    val minAge: Long = 0,
    val baseYear: ZonedDateTime = ZonedDateTime.now()
) {
    val random: Random = Random(seed)
    private val dataset: GDataset

    init {
        val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val postalOffices = objectMapper.readValue(Generator::class.java.getResourceAsStream("/no/postalnumbers.yaml"), Array<GPostalOffice>::class.java)
        val baseinfo = objectMapper.readValue(Generator::class.java.getResourceAsStream("/no/baseinfo.yaml"), Baseinfo::class.java)
        val maleNames = objectMapper.readValue(Generator::class.java.getResourceAsStream("/no/male_names.yaml"), Array<String>::class.java)
        val femaleNames = objectMapper.readValue(Generator::class.java.getResourceAsStream("/no/female_names.yaml"), Array<String>::class.java)
        val surnames = objectMapper.readValue(Generator::class.java.getResourceAsStream("/no/surnames.yaml"), Array<String>::class.java)
        val streets = objectMapper.readValue(Generator::class.java.getResourceAsStream("/no/streets.yaml"), Array<String>::class.java)
        dataset = GDataset(
                postalOffices = postalOffices.toList(),
                baseinfo = baseinfo,
                maleNames = maleNames.toList(),
                femaleNames = femaleNames.toList(),
                surnames = surnames.toList(),
                streets = streets.toList()
        )
    }

    fun mobilePhoneNumber() = random.numberWithFormat(random.nextEntry(dataset.baseinfo.mobilePhoneNumberFormat))
    fun landlinePhoneNumber() = random.numberWithFormat(random.nextEntry(dataset.baseinfo.landlinePhoneNumberFormat))
    fun companyPhoneNumber() = random.numberWithFormat(random.nextEntry(dataset.baseinfo.companyPhoneNumberFormat))

    fun countryCode(): String = "NO" // Static for now

    fun bornDate(): ZonedDateTime = random.dateBetween(baseYear.minusYears(maxAge), baseYear.minusYears(minAge))

    fun firstName(gender: GGender): String = random.nextEntry(if (gender == GGender.Male) { dataset.maleNames } else { dataset.femaleNames })
    fun middleName(gender: GGender): String = firstName(gender)
    fun surname(): String = random.nextEntry(dataset.surnames)

    private val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

    fun personNumber(bornDate: ZonedDateTime, useDNumber: Boolean = false): Long {
        val personDate = bornDate.format(personNumberDateFormat).let {
            if (useDNumber) "${it[0] + 4}${it.substring(1)}" else it
        }
        return (if (bornDate.year >= 2000) (75011..99999) else (11111..50099))
                .map { "$personDate$it" }
                .first {
                    validatePersonAndDNumber(it)
                }
                .toLong()
    }

    fun postalOffice(): GPostalOffice = random.nextEntry(dataset.postalOffices)

    fun streetName(): String = random.nextEntry(dataset.streets)
    fun streetNumber(): Int = if (random.nextInt(4) == 1) { random.nextInt(1, 9999) } else { random.nextInt(1, 80) }
    fun apartmentNumber(): Int = random.nextInt(1, 20) * 1000 + random.nextInt(1, 99)

    fun gender(): GGender = random.nextEntry(GGender.values().toList())

    fun deathDate() = random.dateBetween(ZonedDateTime.now().minusYears(20), ZonedDateTime.now())

    // Probably broken because it has too many types that doesn't make sense for relation in this sense
    fun relationshipStatus() = random.nextEntry(validRelationsWithAnotherPerson)

    fun relations(relationshipStatus: GRelationType?) = if (relationshipStatus == null) {
        listOf()
    } else {
        listOf(GFamilyRelation(relationshipStatus, GPerson(relationshipStatus = relationshipStatus, relations = listOf())))
    }

    fun text(minWords: Long, maxWords: Long): String = (0..(random.nextLong(minWords, maxWords))).joinToString(" ") {
        "word"
    }

    fun companyName(): String = "TODO" // TODO
    fun domainName(): String = "todo.no" // TODO
    fun jobTitle(): String = "TODO" // TODO
}
