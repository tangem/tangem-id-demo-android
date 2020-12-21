package com.tangem.id.demo

import android.util.Base64
import com.tangem.id.extensions.calculateSha3v256
import com.tangem.id.proof.LinkedDataProof.Companion.BASE64_JWS_OPTIONS

class DemoCredentialSubjectFactory(val subjectId: String, val personData: DemoPersonData) {

    val photoHash = Base64.encodeToString(personData.photo.calculateSha3v256(), BASE64_JWS_OPTIONS)

    fun createPhotoCredentialSubject() = mapOf(
        "id" to subjectId,
        "photo" to Base64.encodeToString(personData.photo, Base64.URL_SAFE)
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

    fun createAgeOver21CredentialSubject() = mapOf(
        "id" to subjectId,
        "photoHash" to photoHash
    )
}