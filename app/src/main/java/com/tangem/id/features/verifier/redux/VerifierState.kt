package com.tangem.id.features.verifier.redux

import com.tangem.id.common.redux.*
import org.rekotlin.StateType

data class CredentialStatus(val issuer: Issuer, val verificationStatus: VerificationStatus)

data class Issuer(val address: String, val trusted: Boolean)

enum class VerificationStatus {
    Offline,
    Valid,
    Invalid,
    Revoked,
}

val dummyIssuer = Issuer("someone", false)

data class CredentialStatuses(
    val photo: CredentialStatus = CredentialStatus(dummyIssuer, VerificationStatus.Valid),
    val passport: CredentialStatus = CredentialStatus(dummyIssuer, VerificationStatus.Valid),
    val securityNumber: CredentialStatus = CredentialStatus(dummyIssuer, VerificationStatus.Valid),
    val ageOfMajority: CredentialStatus = CredentialStatus(dummyIssuer, VerificationStatus.Valid),
    val immunity: CredentialStatus = CredentialStatus(dummyIssuer, VerificationStatus.Valid)
)

data class VerifierState(
    val cardId: String? = null,
    val photo: Photo? = Photo(),
    val passport: Passport? = Passport(),
    val securityNumber: SecurityNumber? = SecurityNumber("000-00-000"),
    val ageOfMajority: AgeOfMajority? = AgeOfMajority(true),
    val immunityPassport: ImmunityPassport? = null,
//    val credentialsStatus: List<CredentialStatus> = listOf()
    val credentialStatuses: CredentialStatuses = CredentialStatuses()
) : StateType {
    fun getCredentials() =
        listOfNotNull(photo, passport, securityNumber, ageOfMajority, immunityPassport)
}


