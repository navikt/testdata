package no.nav.datagen

data class Baseinfo(
    val alphabet: String,
    val personalEmails: List<String>,
    val companySuffixes: List<String>,
    val landlinePhoneNumberFormat: List<String>,
    val companyPhoneNumberFormat: List<String>,
    val mobilePhoneNumberFormat: List<String>
)
