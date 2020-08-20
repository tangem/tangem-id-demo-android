package com.tangem.id.demo

import android.content.Context
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.documents.VerifiableCredential.Companion.TANGEM_ETH_CREDENTIAL
import com.tangem.id.extensions.calculateSha3v512
import org.json.JSONArray
import java.time.LocalDate

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

    private fun createAgeOver21Credential(): VerifiableCredential {
        val credentialSubject =
            credentialSubjectFactory.createAgeOver21CredentialSubject()

        val bornDate = personData.born.toDate()!!
        val over21Date = bornDate.plusYears(21)
        val validFrom =
            if (over21Date > LocalDate.now()) "${over21Date}\"${over21Date}T00:00:00Z\"" else null // TODO: maybe use start of the day in current time zone?

        return VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
//            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, TANGEM_AGE_OVER_21_CREDENTIAL),
            validFrom = validFrom
        )
    }

    fun createCredentials(): List<VerifiableCredential> {
        val credentials = listOf(
            createPhotoCredential(),
            createPersonalInformationCredential(),
            createSsnCredential(),
            createAgeOver21Credential()
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
        const val TANGEM_AGE_OVER_21_CREDENTIAL = "TangemAgeOver21Credential"
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
