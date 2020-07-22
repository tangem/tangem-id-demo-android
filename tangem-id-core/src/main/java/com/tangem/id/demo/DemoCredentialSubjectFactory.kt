package com.tangem.id.demo

import com.tangem.id.extensions.calculateSha3v256
import org.apache.commons.codec.binary.Base64
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DemoCredentialSubjectFactory(val subjectId: String, val personData: DemoPersonData) {

    val photoHash = Base64.encodeBase64URLSafeString(personData.photo.calculateSha3v256())

    fun createPhotoCredentialSubject() = mapOf(
        "id" to subjectId,
        "photo" to Base64.encodeBase64URLSafeString(personData.photo)
    )

    fun createPersonalInformationCredentialSubject() = mapOf(
            "id" to subjectId,
            "givenName" to personData.givenName,
            "familyName" to personData.familyName,
            "gender" to personData.gender,
            "born" to personData.born,
            "photoHash" to photoHash
        )

    fun createSsnCredentialSubject() = mapOf(
        "id" to subjectId,
        "ssn" to personData.ssn,
        "photoHash" to photoHash
    )

    fun createAgeOver18CredentialSubject(): Map<String, Any> {
        val credentialSubject = mutableMapOf(
            "id" to subjectId,
            "photoHash" to photoHash
        )
        return credentialSubject
    }
}