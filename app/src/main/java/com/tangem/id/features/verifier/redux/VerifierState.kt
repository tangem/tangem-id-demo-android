package com.tangem.id.features.verifier.redux

import com.tangem.id.R
import com.tangem.id.common.redux.*
import com.tangem.id.demo.VerifiableDemoCredential
import org.rekotlin.StateType

data class CredentialStatus(val issuer: Issuer, val verificationStatus: VerificationStatus)

data class Issuer(val address: String, val trusted: Boolean) {
    fun isTrustedLocalized(): Int =
        if (trusted) {
            R.string.verifier_screen_issuer_trusted
        } else {
            R.string.verifier_screen_issuer_unknown
        }

    fun getColor(): Int = if (trusted) R.color.success else R.color.error
}

enum class VerificationStatus {
    Offline,
    Valid,
    Invalid,
    Revoked;

    fun getLocalizedStatus(): Int =
        when (this) {
            Offline -> R.string.verifier_screen_status_offline
            Valid -> R.string.verifier_screen_status_valid
            Invalid -> R.string.verifier_screen_status_invalid
            Revoked -> R.string.verifier_screen_status_revoked
        }

    fun getColor(): Int =
        when (this) {
            Offline -> R.color.offline
            Valid -> R.color.success
            Invalid -> R.color.error
            Revoked -> R.color.error
        }
}

val dummyIssuer = Issuer("someone", false)

val credentialStatus = CredentialStatus(dummyIssuer, VerificationStatus.Valid)

data class VerifierState(
    val photo: VerifierCredential<Photo>? = VerifierCredential(Photo(), credentialStatus),
    val passport: VerifierCredential<Passport>? = VerifierCredential(Passport(), credentialStatus),
    val securityNumber: VerifierCredential<SecurityNumber>? =
        VerifierCredential(SecurityNumber("000-00-000"), credentialStatus),
    val ageOfMajority: VerifierCredential<AgeOfMajority>? =
        VerifierCredential(AgeOfMajority(true), credentialStatus),
    val immunityPassport: VerifierCredential<ImmunityPassport>? = null,
    val jsonShown: String? = null
) : StateType {
    fun getCredentials() =
        listOfNotNull(photo, passport, securityNumber, ageOfMajority, immunityPassport)
}

data class VerifierCredential<T : Credential>(
    val credential: T,
    val credentialStatus: CredentialStatus
) {
    companion object {
        fun from(demoCredential: VerifiableDemoCredential): VerifierCredential<*>? {

            val issuerAddress = demoCredential.verifiableCredential.issuer
            val issuer = Issuer(issuerAddress, true)
            val verificationStatus = if (demoCredential.verified == false)  {
                VerificationStatus.Invalid
            } else if (demoCredential.verified == true) {
                VerificationStatus.Valid
            } else {
                VerificationStatus.Offline
            }
            val status = CredentialStatus(issuer, verificationStatus)

            return VerifierCredential(Credential.from(demoCredential.decodedCredential), status)
        }
    }
}




