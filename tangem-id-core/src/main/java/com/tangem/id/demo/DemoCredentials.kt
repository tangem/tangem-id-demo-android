package com.tangem.id.demo

import android.content.Context
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.documents.VerifiableCredential.Companion.TANGEM_ETH_CREDENTIAL
import com.tangem.id.documents.VerifiableDocument
import com.tangem.id.extensions.calculateSha3v512
import org.apache.commons.codec.binary.Base64
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DemoCredentialFactory(
    private val issuer: String,
    private val personData: DemoPersonData,
    subjectId: String,
    private val androidContext: Context
) {
    private val credentialSubjectFactory = DemoCredentialSubjectFactory(subjectId, personData)

    private fun createPhotoCredential(): VerifiableCredential {
        val credentialSubject =
            credentialSubjectFactory.createPhotoCredentialSubject()

        return VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
//            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, TANGEM_PHOTO_CREDENTIAL)
        )
    }

    private fun createPersonalInformationCredential(): VerifiableCredential {
        val credentialSubject =
            credentialSubjectFactory.createPersonalInformationCredentialSubject()

        return VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
//            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, TANGEM_PERSONAL_INFORMATION_CREDENTIAL)
        )
    }

    private fun createSsnCredential(): VerifiableCredential {
        val credentialSubject =
            credentialSubjectFactory.createSsnCredentialSubject()

        return VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
//            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, TANGEM_SSN_CREDENTIAL)
        )
    }

    private fun createAgeOver18Credential(): VerifiableCredential {
        val credentialSubject =
            credentialSubjectFactory.createAgeOver18CredentialSubject()

        val bornDate = LocalDate.parse(personData.born, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        val over21Date = bornDate.plusYears(21)
        val validFrom =
            if (over21Date > LocalDate.now()) "${over21Date}\"${over21Date}T00:00:00Z\"" else null // TODO: maybe use start of the day in current time zone?

        return VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
//            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, TANGEM_AGE_OVER_18_CREDENTIAL),
            validFrom = validFrom
        )
    }

    fun createCredentials(): List<VerifiableCredential> {
        val credentials = listOf(
            createPhotoCredential(),
            createPersonalInformationCredential(),
            createSsnCredential(),
            createAgeOver18Credential()
        )

        val credentialJsonArray = JSONArray()
        for (credential in credentials) {
            credentialJsonArray.put(credential.toJSONObject())
        }

        val credentialArrayHash = credentialJsonArray.toString().toByteArray().calculateSha3v512()
        val ethCredentialStatus =
            EthereumAddressService().makeAddress(ByteArray(1) + credentialArrayHash)

        for (credential in credentials) {
            credential.ethCredentialStatus = ethCredentialStatus
        }

        return credentials
    }

    companion object {
        const val TANGEM_PHOTO_CREDENTIAL = "TangemPhotoCredential"
        const val TANGEM_PERSONAL_INFORMATION_CREDENTIAL = "TangemPersonalInformationCredential"
        const val TANGEM_SSN_CREDENTIAL = "TangemSsnCredential"
        const val TANGEM_AGE_OVER_18_CREDENTIAL = "TangemAgeOver18Credential"
    }
}

class DemoPersonData(
    val givenName: String,
    val familyName: String,
    val gender: String,
    val born: String,
    val ssn: String,
    val photo: ByteArray
)


sealed class DemoCredential {
    data class PhotoCredential(val photo: ByteArray) : DemoCredential()
    data class PersonalInfoCredential(
        val name: String,
        val surname: String,
        val gender: String,
        val birthDate: String
    ) : DemoCredential()

    data class SsnCredential(val ssn: String) : DemoCredential()
    data class AgeOfMajorityCredential(val valid: Boolean) : DemoCredential()
    data class CovidCredential(val result: CovidStatus) : DemoCredential()
}

enum class CovidStatus {
    Negative, Positive;

    companion object {
        fun fromString(status: String?): CovidStatus {
            return if (status.isNullOrBlank() || status == "negative") Negative else Positive
        }
    }
}

class VerifiableDemoCredential(
    val verifiableCredential: VerifiableCredential,
    val decodedCredential: DemoCredential,
    val verified: Boolean? = null
) {
    companion object {

        fun from(
            verifiableCredential: VerifiableCredential,
            verified: Boolean? = null
        ): VerifiableDemoCredential? {
            val demoCredential: DemoCredential? =
                when {
                    verifiableCredential.type.contains(DemoCredentialFactory.TANGEM_PHOTO_CREDENTIAL) -> {
                        val photoBase64 =
                            (verifiableCredential.credentialSubject["photo"] as? String)
                        val photo = photoBase64?.let { Base64.decodeBase64(it) }
                        if (photo != null) DemoCredential.PhotoCredential(photo) else null
                    }
                    verifiableCredential.type.contains(DemoCredentialFactory.TANGEM_PERSONAL_INFORMATION_CREDENTIAL) -> {
                        val name = verifiableCredential.credentialSubject["givenName"] as? String
                        val surname =
                            verifiableCredential.credentialSubject["familyName"] as? String
                        val gender = verifiableCredential.credentialSubject["gender"] as? String
                        val birthDate = verifiableCredential.credentialSubject["born"] as? String

                        if (name != null && surname != null && gender != null && birthDate != null) {
                            DemoCredential.PersonalInfoCredential(name, surname, gender, birthDate)
                        } else {
                            null
                        }
                    }
                    verifiableCredential.type.contains(DemoCredentialFactory.TANGEM_AGE_OVER_18_CREDENTIAL) -> {
                        val validFrom = verifiableCredential.validFrom?.substringBefore("T")
                        val dateValidFrom = validFrom?.toDate()
                        val valid = if (dateValidFrom != null) {
                            dateValidFrom < LocalDate.now()
                        } else {
                            true
                        }

                        DemoCredential.AgeOfMajorityCredential(valid)
                    }
                    verifiableCredential.type.contains(DemoCredentialFactory.TANGEM_SSN_CREDENTIAL) -> {
                        val ssn = verifiableCredential.credentialSubject["ssn"] as? String
                        if (ssn != null) DemoCredential.SsnCredential(ssn) else null
                    }
                    verifiableCredential.type.contains(DemoCovidCredential.TANGEM_COVID_CREDENTIAL) -> {
                        val covid = verifiableCredential.credentialSubject["result"] as? String
                        DemoCredential.CovidCredential(CovidStatus.fromString(covid))
                    }
                    else -> null
                }

            return if (demoCredential == null) {
                null
            } else {
                VerifiableDemoCredential(
                    verifiableCredential = verifiableCredential, decodedCredential = demoCredential,
                    verified = verified
                )
            }
        }
    }
}

suspend fun VerifiableDocument.simpleVerify(androidContext: Context): Boolean {
        val result = this.verify(androidContext)
        return result is SimpleResult.Success
}

fun String.toDate(): LocalDate? =
    try {
        LocalDate.parse(this, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    } catch (exception: Exception) {
        null
    }
