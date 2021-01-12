package com.tangem.id.demo

import android.util.Base64
import com.tangem.id.documents.VerifiableCredential
import java.time.LocalDate
import com.microsoft.did.sdk.credential.models.VerifiableCredential as MSVerifiableCredential

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
    data class NinjaCredential(val name: String, val surname: String) : DemoCredential()
}

enum class CovidStatus {
    Negative, Positive;

    companion object {
        fun fromString(status: String?): CovidStatus {
            return if (status.isNullOrBlank() || status == "negative") Negative else Positive
        }
    }
}

fun VerifiableCredential.toDemoCredential(): DemoCredential? {
    return when {
        this.type.contains(DemoCredentialFactory.TANGEM_PHOTO_CREDENTIAL) -> {
            val photoBase64 =
                (this.credentialSubject["photo"] as? String)
            val photo = photoBase64?.let { Base64.decode(it, Base64.URL_SAFE) }
            if (photo != null) DemoCredential.PhotoCredential(photo) else null
        }
        this.type.contains(DemoCredentialFactory.TANGEM_PERSONAL_INFORMATION_CREDENTIAL) -> {
            val name = this.credentialSubject["givenName"] as? String
            val surname =
                this.credentialSubject["familyName"] as? String
            val gender = this.credentialSubject["gender"] as? String
            val birthDate = this.credentialSubject["born"] as? String

            if (name != null && surname != null && gender != null && birthDate != null) {
                DemoCredential.PersonalInfoCredential(name, surname, gender, birthDate)
            } else {
                null
            }
        }
        this.type.contains(DemoCredentialFactory.TANGEM_AGE_OVER_21_CREDENTIAL) -> {
            val validFrom = this.validFrom?.substringBefore("T")
            val dateValidFrom = validFrom?.toDate()
            val valid = if (dateValidFrom != null) {
                dateValidFrom < LocalDate.now()
            } else {
                true
            }

            DemoCredential.AgeOfMajorityCredential(valid)
        }
        this.type.contains(DemoCredentialFactory.TANGEM_SSN_CREDENTIAL) -> {
            val ssn = this.credentialSubject["ssn"] as? String
            if (ssn != null) DemoCredential.SsnCredential(ssn) else null
        }
        this.type.contains(DemoCovidCredential.TANGEM_COVID_CREDENTIAL) -> {
            val covid = this.credentialSubject["result"] as? String
            DemoCredential.CovidCredential(CovidStatus.fromString(covid))
        }
        else -> null
    }
}

fun MSVerifiableCredential.toDemoCredential(): DemoCredential? {
    return try {
        val name = this.contents.vc.credentialSubject["firstName"]
        val surname = this.contents.vc.credentialSubject["lastName"]
        DemoCredential.NinjaCredential(name!!, surname!!)
    } catch (exception: Exception) {
        null
    }
}
