package com.tangem.id.demo

import android.content.Context
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.documents.VerifiableDocument
import org.apache.commons.codec.binary.Base64
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                    verifiableCredential.type.contains(DemoCredentialFactory.TANGEM_AGE_OVER_21_CREDENTIAL) -> {
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
    if (this.contains("/")) {
        try {
            LocalDate.parse(this, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        } catch (exception: Exception) {
            null
        }
    } else {
        try {
            val builder = StringBuilder(this)
            builder.insert(2, "/")
            builder.insert(5, "/")
            LocalDate.parse(builder.toString(), DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        } catch (exception: Exception) {
            null
        }
    }
