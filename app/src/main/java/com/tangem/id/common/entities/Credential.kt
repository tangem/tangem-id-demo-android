package com.tangem.id.common.entities

import android.graphics.Bitmap
import com.tangem.id.R
import com.tangem.id.common.extensions.toBitmap
import com.tangem.id.common.extensions.toDate
import com.tangem.id.demo.CovidStatus
import com.tangem.id.demo.DemoCredential
import com.tangem.id.demo.VerifierDemoCredential

interface Credential {
    fun isDataPresent(): Boolean

    companion object {
        fun from(credential: DemoCredential): Credential {
            return when (credential) {
                is DemoCredential.PhotoCredential -> {
                    val photoInBytes = credential.photo
                    val photoBitmap = photoInBytes.toBitmap()
                    Photo(photoBitmap)
                }
                is DemoCredential.PersonalInfoCredential -> {
                    Passport(
                        credential.name, credential.surname,
                        Gender.valueOf(credential.gender), credential.birthDate
                    )
                }
                is DemoCredential.SsnCredential -> SecurityNumber(credential.ssn)
                is DemoCredential.AgeOfMajorityCredential -> AgeOfMajority(credential.valid)
                is DemoCredential.CovidCredential ->
                    ImmunityPassport(credential.result == CovidStatus.Positive)
                is DemoCredential.VCExpertCredential ->
                    VCExpert(credential.name, credential.surname)
            }
        }
    }
}

data class Photo(val photo: Bitmap? = null) : Credential {
    override fun isDataPresent(): Boolean = photo != null
}

data class Passport(
    val name: String? = null,
    val surname: String? = null,
    val gender: Gender? = null,
    val birthDate: String? = null
) : Credential {
    override fun isDataPresent() =
        !name.isNullOrBlank() && !surname.isNullOrBlank() && isDateValid() == true
                && gender != null

    fun isDateValid(): Boolean? {
        if (birthDate.isNullOrBlank()) return null
        return birthDate.toDate() != null
    }

    fun isInputDataModified(): Boolean {
        return !name.isNullOrBlank() || !surname.isNullOrBlank() || gender != null
                || !birthDate.isNullOrBlank()
    }

    fun dateFormatted(): String? {
        if (isDateValid() != true) return null
        if (birthDate!!.contains("/")) return birthDate
        val builder = StringBuilder(birthDate)
        builder.insert(2, "/")
        builder.insert(5, "/")
        return builder.toString()
    }

    companion object {
        fun from(demoCredential: VerifierDemoCredential): Passport? {
            val credential = demoCredential.decodedCredential
            return if (credential is DemoCredential.PersonalInfoCredential) {
                Passport(
                    credential.name, credential.surname,
                    Gender.valueOf(credential.gender), credential.birthDate
                )
            } else {
                null
            }
        }
    }
}

data class SecurityNumber(val number: String? = null) : Credential {
    fun ssnFormatted(): String? {
        if (!isDataPresent()) return null
        if (number!!.contains("-")) return number
        val builder = StringBuilder(number)
        builder.insert(3, "-")
        builder.insert(6, "-")
        return builder.toString()
    }

    override fun isDataPresent(): Boolean = !number.isNullOrBlank() && number.length == 9
}

data class AgeOfMajority(val valid: Boolean = false) : Credential {
    override fun isDataPresent(): Boolean = true
}

data class ImmunityPassport(val valid: Boolean) : Credential {
    override fun isDataPresent(): Boolean = true
}

data class VCExpert(
    val name: String? = null,
    val surname: String? = null
) : Credential {
    override fun isDataPresent(): Boolean = !name.isNullOrBlank() && !surname.isNullOrBlank()
}

enum class Gender {
    Male, Female, Other;

    fun toLocalizedString(): Int {
        return when (this) {
            Male -> R.string.credential_personal_info_gender_male
            Female -> R.string.credential_personal_info_gender_female
            Other -> R.string.credential_personal_info_gender_other
        }
    }

    companion object {
        private val values = values()
        fun byOrdinal(ordinal: Int): Gender = values[ordinal]
    }
}