package no.nav.testdata.helse

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.datagen.GDoctor
import no.nav.datagen.GPerson
import no.nav.datagen.Generator
import no.nav.datagen.dateBetween
import no.nav.datagen.findMainEmployment
import no.nav.datagen.nextEntry
import no.nav.datagen.nextInt
import no.nav.datagen.nextLong
import no.nav.datagen.withChance
import no.nav.helse.sm2013.Address
import no.nav.helse.sm2013.ArsakType
import no.nav.helse.sm2013.CS
import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.DynaSvarType
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.helse.sm2013.NavnType
import no.nav.helse.sm2013.TeleCom
import no.nav.helse.sm2013.URL
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import java.util.Random
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

private val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

fun ZonedDateTime.toXMLDate(): XMLGregorianCalendar
        = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(this))

fun GPerson.toSM2013(
        doctor: GPerson,
        nameFastlege: String = doctor.fullName,
        navOffice: String = String.format("%04d", Random().nextInt(9999)),
        period: List<GActivityPeriod> = listOf(GActivityPeriod(generator)),
        startDate: ZonedDateTime = ZonedDateTime.now().minusYears(generator.random.nextLong(0, 30)),
        senderSystemName: String = "NAV Test generator",
        senderSystemVersion: String = "1.0",
        forecast: GForecast = GForecast(generator, employments.isNotEmpty()),
        medicalReasoning: GMedicalReasoning = GMedicalReasoning(generator),
        answers: List<GAnswer> = GQuestion.values().filter { it.group == GQuestionGroup.INFORMATION_SURROUNDING_ACTIVITY_REQUIREMENT }.map { GAnswer(it, generator.text(10, 100)) },
        measuresNAV: String = generator.text(10, 100),
        measuresWorkplace: String = generator.text(10, 100),
        otherMeasures: String = generator.text(10, 100),
        receivedInvitationDialogMeeting1: Boolean = generator.random.nextBoolean(),
        attendedDialogMeeting1: Boolean = receivedInvitationDialogMeeting1 && generator.random.nextInt(3) != 1,
        receivedFollowupPlan: Boolean = generator.random.nextBoolean(),
        reasonNotAttendingDialogMeeting: String? = attendedDialogMeeting1.onTrue { generator.text(10, 100) },
        aidFromNavDescription: String? = generator.random.withChance(1, 10) { generator.text(10, 100) },
        aidFromNavImmediately: Boolean = aidFromNavDescription != null && generator.random.nextInt() == 1,
        messageToEmployer: String? = generator.random.withChance(1, 10) { generator.text(10, 100) },
        patientContactDate: ZonedDateTime? = generator.random.withChance(3, 4) { ZonedDateTime.now().minusDays(generator.random.nextLong(3, 30)) },
        patientContactProcessedDate: ZonedDateTime? = patientContactDate?.let { generator.random.dateBetween(patientContactDate, ZonedDateTime.now()) },
        reasonForNotContactingPatient: String? = patientContactDate?.let { generator.text(10, 100) },
        mainDiagnose: Kodeverk = generator.random.nextEntry(ICD10.values().flatMap { ICPC2.values().toList() }),
        biDiagnosis: List<Kodeverk> = listOf(generator.random.nextEntry(ICD10.values().flatMap { ICPC2.values().toList() }))
): HelseOpplysningerArbeidsuforhet {
    val data = this
    val doctorExt = doctor.ext<GDoctor>()
    return HelseOpplysningerArbeidsuforhet().apply {
        regelSettVersjon = "1"
        syketilfelleStartDato = startDate.toXMLDate()
        pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
            fodselsnummer = GPersonIdent.FNR.toIdent(personNumber.toString())
            navn = NavnType().apply {
                fornavn = data.firstName
                mellomnavn = data.middleName
                etternavn = data.surname
            }
            navKontor = navOffice
            navnFastlege = nameFastlege
            kontaktInfo.addAll(phoneNumbersToTeleComList())
        }
        medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
            annenFraversArsak = ArsakType().apply {
                arsakskode.addAll(medicalReasoning.otherAbsenceReasons.map { it.toCS() })
                beskriv = medicalReasoning.otherAbsenceReasonDescription
            }
            // TODO: Hoveddiagnose og bidiagnose
            hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = mainDiagnose.toCV()
            }
            biDiagnoser = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.BiDiagnoser().apply {
                diagnosekode.addAll(biDiagnosis.map { it.toCV() })
            }
            isSkjermesForPasient = medicalReasoning.hiddenFromPatient
            isSvangerskap = medicalReasoning.pregnancy
            isYrkesskade = medicalReasoning.workRelatedInjuryDate != null
            yrkesskadeDato = medicalReasoning.workRelatedInjuryDate?.toXMLDate()
        }
        arbeidsgiver = HelseOpplysningerArbeidsuforhet.Arbeidsgiver().apply {
            val mainEmployment = data.employments.findMainEmployment()
            harArbeidsgiver = when {
                data.employments.isEmpty() -> HasEmployer.NO_EMPLOYER
                data.employments.size == 1 -> HasEmployer.SINGLE_EMPLOYER
                else -> HasEmployer.MULTIPLE_EMPLOYERS
            }.toCS()
            if (mainEmployment != null) {
                stillingsprosent = mainEmployment.percentage
                yrkesbetegnelse = mainEmployment.jobTitle
                navnArbeidsgiver = mainEmployment.workplace.name
            }
        }
        behandler = HelseOpplysningerArbeidsuforhet.Behandler().apply {
            adresse = Address().apply {
                val mainAddress = doctor.addressLines.first()
                country = CS().apply {
                    v = mainAddress.countryCode
                }
                city = mainAddress.postalOffice.postalName
                postalCode = mainAddress.postalOffice.postalNumber.toString()
                postbox = mainAddress.postbox
                streetAdr = mainAddress.shortAddress
            }
            id.addAll(listOfNotNull(
                    GPersonIdent.FNR.toIdent(personNumber.toString()),
                    GPersonIdent.HER.toIdent(doctorExt.herId),
                    GPersonIdent.HPR.toIdent(doctorExt.hprNumber)
            ))
            navn = NavnType().apply {
                fornavn = doctor.firstName
                mellomnavn = doctor.middleName
                etternavn = doctor.surname
            }
            kontaktInfo.addAll(doctor.phoneNumbersToTeleComList())
        }
        aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
            periode.addAll(period.map { activityPeriod ->
                HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = activityPeriod.periodStart.toXMLDate()
                    periodeTOMDato = activityPeriod.periodEnd.toXMLDate()
                    // TODO: Kun en type
                    if (activityPeriod.notPossibleHealth != null || activityPeriod.notPossibleWorkplace != null) {
                        aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                            arbeidsplassen = activityPeriod.notPossibleWorkplace?.toArsakType()
                            medisinskeArsaker = activityPeriod.notPossibleHealth?.toArsakType()
                        }
                    }
                    avventendeSykmelding = activityPeriod.pendingSickLeaveInputToEmployer?.let {
                        HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AvventendeSykmelding().apply {
                            innspillTilArbeidsgiver = it
                        }
                    }
                    isReisetilskudd = activityPeriod.travelSubsidy
                    behandlingsdager = activityPeriod.numberOfTreatmentWeekdays?.let {
                        HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.Behandlingsdager().apply {
                            antallBehandlingsdagerUke = it
                        }
                    }
                    gradertSykmelding = activityPeriod.partialSickLeave?.let {
                        HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.GradertSykmelding().apply {
                            isReisetilskudd = it.travelSubsidy
                            sykmeldingsgrad = it.sickLeaveGrade
                        }
                    }
                }
            })
        }
        avsenderSystem = HelseOpplysningerArbeidsuforhet.AvsenderSystem().apply {
            systemNavn = senderSystemName
            systemVersjon = senderSystemVersion
        }
        prognose = HelseOpplysningerArbeidsuforhet.Prognose().apply {
            beskrivHensynArbeidsplassen = forecast.considerationsWorkplace
            erIArbeid = forecast.forecastInWork?.let {
                HelseOpplysningerArbeidsuforhet.Prognose.ErIArbeid().apply {
                    arbeidFraDato = forecast.forecastInWork.canWorkFromDate.toXMLDate()
                    vurderingDato = forecast.forecastInWork.evaluationDate.toXMLDate()
                    isAnnetArbeidPaSikt = forecast.forecastInWork.canDoOtherWorkInFuture
                    isEgetArbeidPaSikt = forecast.forecastInWork.canDoIndividualWorkInFuture
                }
            }
            erIkkeIArbeid = forecast.forecastNotInWork?.let {
                HelseOpplysningerArbeidsuforhet.Prognose.ErIkkeIArbeid().apply {
                    arbeidsforFraDato = forecast.forecastNotInWork.canWorkFromDate.toXMLDate()
                    isArbeidsforPaSikt = forecast.forecastNotInWork.canWorkInFuture
                    vurderingDato = forecast.forecastNotInWork.feedbackDate.toXMLDate()
                }
            }
        }
        utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
            spmGruppe.addAll(answers
                    .groupBy { it.question.group }
                    .map {  (group, questions) ->
                        HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe().apply {
                            spmGruppeId = group.groupId
                            spmGruppeTekst = group.groupText
                            spmSvar.addAll(questions.map {
                                DynaSvarType().apply {
                                    spmId = it.question.questionId
                                    spmTekst = it.question.question
                                    restriksjon = DynaSvarType.Restriksjon().apply {
                                        restriksjonskode.addAll(it.question.restrictions.map(Kodeverk::toCS))
                                    }
                                    svarTekst = it.answer
                                }
                            })
                        }
                    })
        }
        tiltak = HelseOpplysningerArbeidsuforhet.Tiltak().apply {
            tiltakNAV = measuresNAV
            tiltakArbeidsplassen = measuresWorkplace
            andreTiltak = otherMeasures
        }
        oppfolgingsplan = HelseOpplysningerArbeidsuforhet.Oppfolgingsplan().apply {
            isInnkaltDialogmote1 = receivedInvitationDialogMeeting1
            isDeltattDialogmote1 = attendedDialogMeeting1
            arsakIkkeDeltatt = reasonNotAttendingDialogMeeting
            isMottattOppfolgingsplan = receivedFollowupPlan
        }
        meldingTilNav = HelseOpplysningerArbeidsuforhet.MeldingTilNav().apply {
            beskrivBistandNAV = aidFromNavDescription
            isBistandNAVUmiddelbart = aidFromNavImmediately
        }
        meldingTilArbeidsgiver = messageToEmployer
        kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
            begrunnIkkeKontakt = reasonForNotContactingPatient
            kontaktDato = patientContactDate?.toXMLDate()
            behandletDato = patientContactProcessedDate?.toXMLDate()
        }
    }
}

fun <T> Boolean.onTrue(callback: () -> T): T? = if (this) { callback() } else { null }
fun <T> Boolean.onFalse(callback: () -> T): T? = if (this) { null } else { callback() }

data class GMedicalReasoning(
    @JsonIgnore
    val generator: Generator,
    val otherAbsenceReasons: List<OtherAbsenceReason> = listOf(generator.random.nextEntry(OtherAbsenceReason.values().toList())),
    val otherAbsenceReasonDescription: String = generator.text(10, 100),
    val hiddenFromPatient: Boolean = generator.random.nextInt(5) == 1,
    val pregnancy: Boolean = generator.random.nextInt(5) == 1,
    val workRelatedInjuryDate: ZonedDateTime? = generator.random.withChance(1, 3) { ZonedDateTime.now().minusDays(generator.random.nextLong(1, 10)) }
)

data class GForecastNotInWork(
    @JsonIgnore
    val generator: Generator,
    val canWorkInFuture: Boolean = generator.random.nextBoolean(),
    val canWorkFromDate: ZonedDateTime = ZonedDateTime.now().plusDays(generator.random.nextLong(3, 30)),
    val feedbackDate: ZonedDateTime = ZonedDateTime.now().plusDays(generator.random.nextLong(4, 15))
)

data class GForecastInWork(
    @JsonIgnore
    val generator: Generator,
    val canWorkFromDate: ZonedDateTime = ZonedDateTime.now().plusDays(generator.random.nextLong(3, 30)),
    val evaluationDate: ZonedDateTime = ZonedDateTime.now(),
    val canDoOtherWorkInFuture: Boolean = generator.random.nextBoolean(),
    val canDoIndividualWorkInFuture: Boolean = generator.random.nextBoolean()
)

data class GForecast(
    @JsonIgnore
    val generator: Generator,
    @JsonIgnore
    val inWork: Boolean,
    val considerationsWorkplace: String = generator.text(10, 100),
    val forecastInWork: GForecastInWork? = inWork.onTrue { GForecastInWork(generator) },
    val forecastNotInWork: GForecastNotInWork? = inWork.onFalse { GForecastNotInWork(generator) }
)

fun GPerson.phoneNumbersToTeleComList() = listOfNotNull(
        GPhoneType.MC.toTeleCom(mobilePhoneNumber),
        landlinePhoneNumber?.let { GPhoneType.HP.toTeleCom(it) }
)

fun GActivityNotPossibleType.toArsakType() = ArsakType().apply {
    arsakskode.addAll(reasons.map { kodeVerk -> kodeVerk.toCS() })
    beskriv = description
}

data class GActivityPeriod(
    @JsonIgnore
    val generator: Generator = Generator(),
    @JsonIgnore
    val activityType: GActivityType? = generator.random.nextEntry(GActivityType.values().toList()),
    val periodStart: ZonedDateTime = ZonedDateTime.now().minusDays(generator.random.nextLong(0, 30)),
    val periodEnd: ZonedDateTime = periodStart.plusDays(generator.random.nextLong(0, 30)),
    val notPossibleWorkplace: GActivityNotPossibleType? = activityType?.onSame(GActivityType.ACTIVITY_NOT_POSSIBLE) {
        generator.random.withChance(1, 2) {
            GActivityNotPossibleType(listOf(generator.random.nextEntry(GWorkplaceReason.values().toList())), generator.text(10, 100))
        }
    },
    val notPossibleHealth: GActivityNotPossibleType? = activityType?.onSame(GActivityType.ACTIVITY_NOT_POSSIBLE) {
        if (notPossibleWorkplace == null) {
            GActivityNotPossibleType(listOf(generator.random.nextEntry(GWorkplaceReason.values().toList())), generator.text(10, 100))
        } else {
            null
        }
    },
    val partialSickLeave: GPartialSickLeave? = activityType?.onSame(GActivityType.PARTIAL_SICK_LEAVE) {
        GPartialSickLeave(generator)
    },
    val numberOfTreatmentWeekdays: Int? = activityType?.onSame(GActivityType.TREATMENT_DAYS) { generator.random.nextInt(1, 10) },
    val travelSubsidy: Boolean? = activityType?.onSame(GActivityType.TRAVEL_SUBSIDY) { generator.random.nextBoolean() },
    val pendingSickLeaveInputToEmployer: String? = activityType?.onSame(GActivityType.PENDING_SICK_LEAVE) { generator.text(10, 100) }
)

enum class GActivityType {
    ACTIVITY_NOT_POSSIBLE,
    PARTIAL_SICK_LEAVE,
    TREATMENT_DAYS,
    TRAVEL_SUBSIDY,
    PENDING_SICK_LEAVE
}

fun <T> GActivityType.onSame(type: GActivityType, callback: () -> T): T? = if (type == this) { callback() } else { null }

data class GPartialSickLeave(
        @JsonIgnore
        val generator: Generator,
        val travelSubsidy: Boolean = generator.random.nextInt(3) == 1,
        val sickLeaveGrade: Int = generator.random.nextInt(10, 100)
)

data class GActivityNotPossibleType(
        val reasons: List<Kodeverk>,
        val description: String
)

interface Kodeverk {
    val codeValue: String
    val text: String
    val oid: String
}

enum class GQuestionGroup(val groupId: String, val groupText: String, val description: String, val required: Boolean = true) {
    INFORMATION_SURROUNDING_ACTIVITY_REQUIREMENT("6.3", "Opplysninger ved vurdering av aktivitetskravet", "Opplysninger ved vurdering av aktivitetskravet"),
    HEALTH_INFORMATION_AT_17_WEEKS("6.4", "Helseopplysninger ved 17 uker", "Utdypende opplysninger ved uke 17 (til NAVs vurdering av oppfølgingen)"),
    HEALTH_INFORMATION_AT_39_WEEKS("6.5", "Beskriv pågående og planlagt henvisning, utredning og/eller behandling", "Beskriv pågående og planlagt henvisning, utredning og/eller behandling"),
    HEALTH_INFORMATION_AAP("6.6", "Helseopplysninger dersom pasienten søker om AAP", "Helseopplysninger dersom pasienten søker om AAP", required = false)
}

enum class GQuestionRestriction(override val codeValue: String, override val text: String, override val oid: String = "2.16.578.1.12.4.1.1.8134") : Kodeverk {
    RESTRICTED_FOR_EMPLOYER("A", "Informasjonen skal ikke vises arbeidsgiver"),
    RESTRICTED_FOR_PATIENT("P", "Informasjonen skal ikke vises pasient"),
    RESTRICTED_FOR_NAV("N", "Informasjonen skal ikke vises NAV")
}

enum class GQuestion(val group: GQuestionGroup, val questionId: String, val restrictions: List<GQuestionRestriction>, val question: String) {
    DESCRIBE_HEALTH_HISTORY(GQuestionGroup.INFORMATION_SURROUNDING_ACTIVITY_REQUIREMENT, "6.3.1", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Beskriv kort sykehistorie, symptomer og funn i dagens situasjon"),
    DESCRIBE_ONGOING_OR_PLANNED_TREATMENT(GQuestionGroup.INFORMATION_SURROUNDING_ACTIVITY_REQUIREMENT, "6.3.2", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Beskriv pågående og planlagt henvisning, utredning og/eller behandling. Lar dette seg kombinere med delvis arbeid?"),
    DESCRIBE_HEALTH_HISTORY_17_WEEKS(GQuestionGroup.HEALTH_INFORMATION_AT_17_WEEKS, "6.4.1", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Beskriv kort sykehistorie, symptomer og funn i dagens situasjon"),
    DESCRIBE_ONGOING_OR_PLANNED_TREATMENT_17_WEEKS(GQuestionGroup.HEALTH_INFORMATION_AT_17_WEEKS, "6.4.2", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Beskriv pågående og planlagt henvisning, utredning og/eller behandling"),
    CONDITION_FOR_GETTING_BACK_INTO_WORK(GQuestionGroup.HEALTH_INFORMATION_AT_17_WEEKS, "6.4.3", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Beskriv pågående og planlagt henvisning, utredning og/eller behandling"),
    DESCRIBE_HEALTH_HISTORY_39_WEEKS(GQuestionGroup.HEALTH_INFORMATION_AT_39_WEEKS, "6.5.1", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Beskriv kort sykehistorie, symptomer og funn i dagens situasjon."),
    HOW_IT_IMPACTS_ABILITY_TO_WORK(GQuestionGroup.HEALTH_INFORMATION_AT_39_WEEKS, "6.5.2", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Hvordan påvirker dette funksjons-/arbeidsevnen?"),
    DESCRIBE_ONGOING_OR_PLANNED_TREATMENT_39_WEEKS(GQuestionGroup.HEALTH_INFORMATION_AT_39_WEEKS, "6.5.3", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Beskriv pågående og planlagt henvisning, utredning og/eller medisinsk behandling"),
    WILL_WORK_RELATED_ACTIVITY_OR_MEDICAL_TREATMENT_IMPROVE_ABILITY_TO_WORK(GQuestionGroup.HEALTH_INFORMATION_AT_39_WEEKS, "6.5.4", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Kan arbeidsevnen bedres gjennom medisinsk behandling og/eller arbeidsrelatert aktivitet? I så fall hvordan? Angi tidsperspektiv"),
    WHAT_PERSONAL_WORK_CAN_PATIENT_DO_TODAY_AND_NEAR_FUTURE(GQuestionGroup.HEALTH_INFORMATION_AAP, "6.6.1", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Hva antar du at pasienten kan utføre av eget arbeid/arbeidsoppgaver i dag eller i nær framtid?"),
    WHAT_ALTERNATIVE_PERSONAL_WORK_CAN_PATIENT_DO_IF_NOT_THE_SAME(GQuestionGroup.HEALTH_INFORMATION_AAP, "6.6.2", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Hvis pasienten ikke kan gå tilbake til eget arbeid, hva antar du at pasienten kan utføre av annet arbeid/arbeidsoppgaver?"),
    HOW_DOES_THE_ILLNESS_IMPACT_ABILITY_TO_WORK(GQuestionGroup.HEALTH_INFORMATION_AAP, "6.6.3", listOf(GQuestionRestriction.RESTRICTED_FOR_EMPLOYER), "Hvilken betydning har denne sykdommen for den nedsatte arbeidsevnen?")
}

data class GAnswer(val question: GQuestion, val answer: String)

enum class GPhoneType(override val codeValue: String, override val text: String, override val oid: String = "2.16.578.1.12.4.1.1.8110") : Kodeverk {
    HP("HP", "Hovedtelefon"),
    MC("MC", "Mobiltelefon"),
    MV("MV", "Ferietelefon"),
    F("F", "Faks"),
    PG("PG", "Personsøker"),
    AS("AS", "Telefonsvarer"),
    WP("WP", "Arbeidsplass"),
    WC("WC", "Arbeidsplass, sentralbord"),
    WD("WD", "Arbeidsplass, direktenummer"),
    EC("EC", "Nødnummer")
}

fun GPhoneType.toTeleCom(value: String) = TeleCom().apply {
    typeTelecom = toCS()
    teleAddress = URL().apply {
        v = value
    }
}

enum class GWorkplaceReason(override val codeValue: String, override val text: String, override val oid: String = "2.16.578.1.12.4.1.1.8132") : Kodeverk {
    MISSING_WORKPLACE_ADAPTATION("1", "Manglende tilrettelegging på arbeidsplassen"),
    OTHER("9", "Annet")
}

enum class GHealthReason(override val codeValue: String, override val text: String, override val oid: String = "2.16.578.1.12.4.1.1.8133") : Kodeverk {
    CONDITION_COMPLICATES_ACTIVITY("1", "Helsetilstanden hindrer pasienten i å være i aktivitet"),
    ACTIVITY_WILL_WORSEN_HEALTH_CONDITION("2", "Aktivitet vil forverre helsetilstanden"),
    ACTIVITY_WILL_STALL_OR_DELAY_IMPROVEMENT_OF_HEALTH_CONDITION("3", "Aktivitet vil hindre/forsinke bedring av helsetilstanden"),
    OTHER("9", "Annet")
}

enum class GPersonIdent(override val codeValue: String, override val text: String, val description: String, override val oid: String = "2.16.578.1.12.4.1.1.8116") : Kodeverk {
    FNR("FNR", "Fødselsnummer", "Norsk fødselsnummer"),
    DNR("DNR", "D-nummer", "Personer i kontakt med norske myndigheter uten norsk fødselsnummer"),
    HNR("HNR", "H-nummer", "Nødnummer"),
    HPR("HPR", "HPR-nummer", "Id Helsepersonellregisteret"),
    HER("HER", "HER-id", "Rollebasert id i NHN Adresseregister"),
    PNR("PNR", "Passnummer", "Passnummer"),
    SEF("SEF", "Svensk personnummer", "Svensk personnummer"),
    DKF("DKF", "Dansk personnummer", "Dansk personnummer"),
    SSN("SSN", "Social security number", "Social security number"),
    FPN("FPN", "Forsikringspolise nummer", "Forsikringspolise nummer"),
    UID("UID", "Utenlandsk identifikasjon", "Utenlandsk identifikasjon"),
    FHN("FHN", "Felles hjelpenummer", "Felles hjelpenummer"),
    XXX("XXX", "Annet", "Annet")
}

enum class OtherAbsenceReason(override val codeValue: String, override val text: String, override val oid: String = "2.16.578.1.12.4.1.1.8131"): Kodeverk {
    APPROVED_HEALTH_INSTITUTION("1", "Når vedkommende er innlagt i en godkjent helseinstitusjon"),
    TREATMENT_MAKES_IT_NECESSARY_TO_NOT_WORK("2", "Når vedkommende er under behandling og legen erklærer at behandlingen gjør det nødvendig at vedkommende ikke arbeider"),
    PARTICIPANT_OF_LABOUR_ORIENTED_MEASURES("3", "Når vedkommende deltar på et arbeidsrettet tiltak"),
    RECEIVES_COMPENSATION_FOR_EDUCATIONAL_MEASURES("4", "Når vedkommende på grunn av sykdom, skade eller lyte får tilskott når vedkommende på grunn av sykdom, skade eller lyte får tilskott"),
    REQUIRES_CONTROL_EXAMINATION("5", "Når vedkommende er til nødvendig kontrollundersøkelse som krever minst 24 timers fravær, reisetid medregnet"),
    RISK_OF_INFECTING_OTHERS("6", "Når vedkommende myndighet har nedlagt forbud mot at han eller hun arbeider på grunn av smittefare"),
    ABORTION("7", "Når vedkommende er arbeidsufør som følge av svangerskapsavbrudd"),
    INFERTILITY("8", "Når vedkommende er arbeidsufør som følge av behandling for barnløshet"),
    DONOR("9", "Når vedkommende er donor eller er under vurdering som donor"),
    TREATMENT_ASSOCIATED_WITH_STERILIZATION("10", "Når vedkommende er arbeidsufør som følge av behandling i forbindelse med sterilisering")
}


enum class HasEmployer(override val codeValue: String, override val text: String, override val oid: String = "2.16.578.1.12.4.1.1.8130") : Kodeverk {
    SINGLE_EMPLOYER("1", "Én arbeidsgiver"),
    MULTIPLE_EMPLOYERS("2", "Flere arbeidsgivere"),
    NO_EMPLOYER("3", "Ingen arbeidsgiver")
}

fun Kodeverk.toCV() = CV().apply {
    this.s = oid
    this.dn = text
    this.v = codeValue
}

fun Kodeverk.toCS() = CS().apply {
    this.dn = text
    this.v = codeValue
}

fun GPersonIdent.toIdent(value: String) = Ident().apply {
    typeId = toCV()
    id = value
}
