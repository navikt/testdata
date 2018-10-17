package no.nav.datagen

import com.fasterxml.jackson.annotation.JsonIgnore
import java.lang.RuntimeException
import java.time.ZonedDateTime

data class GDoctor(
    @JsonIgnore
    val generator: Generator = Generator(),
    val hprNumber: String = generator.random.nextLong(1000000, 9999999).toString(),
    val herId: String = generator.random.nextLong(1000000, 9999999).toString()
)

data class GPerson(
    @JsonIgnore
    val generator: Generator = Generator(),
    val deathDate: ZonedDateTime? = generator.random.withChance(1, 20) { generator.deathDate() },
    val gender: GGender = generator.gender(),
    val firstName: String = generator.firstName(gender),
    val middleName: String? = generator.random.withChance(1, 4) { generator.middleName(gender) },
    val surname: String = generator.surname(),
    val bornDate: ZonedDateTime = generator.bornDate(),
    val citizenshipCountry: String = generator.countryCode(),
    val personNumber: Long = generator.personNumber(bornDate),
    val addressLines: List<GAddress> = (1..generator.random.nextInt(1, 5)).map { GAddress(generator) },
    val mobilePhoneNumber: String = generator.mobilePhoneNumber(),
    val landlinePhoneNumber: String? = generator.random.withChance(1, 10) { generator.landlinePhoneNumber() },
    val relationshipStatus: GRelationType? = generator.random.withChance(1, 2) { generator.relationshipStatus() },
    val relations: List<GFamilyRelation> = generator.relations(relationshipStatus),
    val extensions: List<Any> = listOf(),
    val employments: List<GEmployment> = listOf(GEmployment(generator))
) {
    val fullName = if (middleName == null) { "$firstName $surname" } else { "$firstName $middleName $surname" }

    inline fun <reified T> ext(): T = extOrNull<T>() ?: throw RuntimeException("You need to register the extension ${T::class.java.canonicalName}")
    inline fun <reified T> extOrNull(): T? = extensions.firstOrNull { it is T } as T?
}

fun List<GEmployment>.findMainEmployment() = sortedBy { it.percentage }.firstOrNull()

data class GEmployment(
    @JsonIgnore
    val generator: Generator = Generator(),
    val percentage: Int = generator.random.withChance(1, 3) { generator.random.nextInt(10, 100) } ?: 100,
    val jobTitle: String = generator.jobTitle(),
    val workplace: GCompany = GCompany(generator)
)

data class GAddress(
        @JsonIgnore
    val generator: Generator = Generator(),
        val countryCode: String = generator.countryCode(),
        val postalOffice: GPostalOffice = generator.postalOffice(),
        val streetName: String = generator.streetName(),
        val streetNumber: Int = generator.streetNumber(),
        val appartmentNumber: Int = generator.apartmentNumber(),
        val postbox: String = "${generator.apartmentNumber()} ${generator.streetName()}",
        val houseLetter: Char? = generator.random.withChance(1, 2) {
        (generator.random.nextInt(20) + 'A'.toInt()).toChar()
    }
) {
    val shortAddress = "$streetName $streetNumber"
    val fullAddress = "$shortAddress ${postalOffice.postalNumber} ${postalOffice.postalName}"
}

data class GCompany(
    @JsonIgnore
    val generator: Generator,
    val name: String = generator.companyName(),
    val domainName: String = generator.domainName(),
    val address: GAddress = GAddress(generator)
)

data class GFamilyRelation(
        val relationType: GRelationType,
        val person: GPerson
)

enum class GRelationType {
    FATHER,
    MOTHER,
    CHILD,
    ADOPTIVE_CHILD,
    FOSTER_MOTHER,
    FOSTER_FATHER,
    WIDOW,
    MARRIED,
    SURVIVING_PARTNER,
    MARRIED_LIVES_SEPARATED,
    REGISTERED_PARTNER,
    COHABITANT,
    SEPARATED,
    DIVORCED,
    DIVORCED_PARTNER,
    UNMARRIED
}

val validRelationsWithAnotherPerson = listOf(
        GRelationType.FATHER,
        GRelationType.MOTHER,
        GRelationType.CHILD,
        GRelationType.ADOPTIVE_CHILD,
        GRelationType.FOSTER_MOTHER,
        GRelationType.FOSTER_FATHER,
        GRelationType.MARRIED,
        GRelationType.SURVIVING_PARTNER,
        GRelationType.COHABITANT,
        GRelationType.SEPARATED,
        GRelationType.DIVORCED,
        GRelationType.DIVORCED_PARTNER
)

enum class GGender {
    Male,
    Female
}

data class GPostalOffice(
        val postalName: String,
        val postalNumber: Int
)

data class GDataset(
        val postalOffices: List<GPostalOffice>,
        val baseinfo: Baseinfo,
        val maleNames: List<String>,
        val femaleNames: List<String>,
        val surnames: List<String>,
        val streets: List<String>
)
