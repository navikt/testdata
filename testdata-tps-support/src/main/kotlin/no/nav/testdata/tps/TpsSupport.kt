package no.nav.testdata.tps

import no.nav.datagen.GGender
import no.nav.datagen.GPerson
import no.nav.datagen.GRelationType
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoennstyper
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Sivilstand
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Sivilstander
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Statsborgerskap
import no.nav.tjeneste.virksomhet.person.v3.informasjon.UstrukturertAdresse
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

fun GPerson.toTPSPerson(withRelations: Boolean, sperrekode: Sperrekode? = null): Person = Person()
        .withAktoer(PersonIdent()
                .withIdent(NorskIdent()
                        .withType(Personidenter().withValue("FNR"))
                        .withIdent(personNumber.toString())))
        .withPersonnavn(Personnavn()
                .withFornavn(firstName)
                .withMellomnavn(middleName)
                .withEtternavn(surname)
                .withSammensattNavn(fullName))
        .withBostedsadresse(Bostedsadresse()
                .withStrukturertAdresse(
                        Gateadresse()
                                .withGatenavn(addressLines[0].streetName)
                                .withGatenummer(addressLines[0].streetNumber)
                                .withHusbokstav(addressLines[0].houseLetter?.toString())
                                // .withHusnummer()
                                .withLandkode(Landkoder().withValue(addressLines[0].countryCode))
                ))
        .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue(when (gender) {
            GGender.Male -> "M"
            GGender.Female -> "K"
            else -> "U"
        })))
        .withFoedselsdato(Foedselsdato().withFoedselsdato(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(bornDate))))
        .withPostadresse(Postadresse().withUstrukturertAdresse(UstrukturertAdresse()
                .withAdresselinje1(addressLines[0].fullAddress)))
        .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue(citizenshipCountry)))
        .apply {
            if (deathDate != null) {
                doedsdato = Doedsdato().withDoedsdato(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(deathDate)))
            }
        }
        .withSivilstand(Sivilstand().withSivilstand(Sivilstander().withValue(relationshipStatus.toKodeverk())))
        .apply {
            if (withRelations) {
                harFraRolleI.addAll(relations.map {
                    Familierelasjon()
                            .withTilRolle(Familierelasjoner().withValue(it.relationType.toKodeverk()))
                            .withTilPerson(it.person.toTPSPerson(false))
                })
            }
        }
        .withDiskresjonskode(Diskresjonskoder().withValue(sperrekode?.sperrekode))

enum class Sperrekode(val sperrekode: String) {
    HIGHLY_CONFIDENTIAL("SPSF"), // Sperret adresse, strengt fortrolig
    CONFIDENTIAL("SPSO") // Sperret adresse, fortrolig
}

fun GRelationType?.toKodeverk() = when (this) {
    GRelationType.FATHER -> "FARA"
    GRelationType.MOTHER -> "MORA"
    GRelationType.CHILD -> "BARN"
    GRelationType.ADOPTIVE_CHILD -> "FOBA"
    GRelationType.FOSTER_FATHER -> "FOFA"
    GRelationType.FOSTER_MOTHER -> "FOMO"
    GRelationType.WIDOW -> "ENKE"
    GRelationType.MARRIED -> "GIFT"
    GRelationType.SURVIVING_PARTNER -> "GJPA" // Gjennlevende partner
    GRelationType.MARRIED_LIVES_SEPARATED -> "GLAD"
    GRelationType.REGISTERED_PARTNER -> "REPA"
    GRelationType.COHABITANT -> "SAMB"
    GRelationType.SEPARATED -> "SEPA"
    GRelationType.DIVORCED -> "SKIL"
    GRelationType.DIVORCED_PARTNER -> "SKPA"
    GRelationType.UNMARRIED -> "UGIF"
    else -> "NULL"
}
