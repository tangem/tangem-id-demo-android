package com.tangem.id

fun createPersonCredentialSubject(
    subjectId: String,
    personData: TangemPersonData,
    photoHashBase64Url: String
): Map<String, Any> {
    return mapOf(
        "id" to subjectId,
        "dln" to personData.dln,
        "name" to personData.name,
        "gender" to personData.gender,
        "born" to personData.born,
        "photoSha3-256" to photoHashBase64Url
    )
}

fun createPhotoCredentialSubject(subjectId: String, photoBase64Url: String): Map<String, Any> {
    return mapOf(
        "id" to subjectId,
        "photo" to photoBase64Url
    )
}

fun createAgeOver18CredentialSubject(subjectId: String, photoHashBase64Url: String): Map<String, Any> {
    return mapOf(
        "id" to subjectId,
        "photoHash" to photoHashBase64Url
    )
}

data class TangemPersonData(
    val dln: String,
    val name: String,
    val gender: String,
    val born: String
)