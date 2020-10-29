package com.tangem.id.proofdemo

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.documents.VerifiableCredential
import org.stellar.sdk.KeyPair

class CertificateCredential(
    credentialSubject: Map<String, String>,
    issuer: String,
    credentialStatus: Map<String, String>,
    extraContexts: Collection<String>? = null,
    extraTypes: Collection<String>? = null
) : VerifiableCredential(
    credentialSubject = credentialSubject,
    issuer = issuer,
    extraContexts = listOf(CONTEXT),
    extraTypes = listOf(TYPE),
    credentialStatus = credentialStatus
) {
    init {
        if (extraContexts != null) context.addAll(extraContexts)
        if (extraTypes != null) type.addAll(extraTypes)
    }

    suspend fun verify(): Result<CertificateVerificationResult> {
        val subjectId = credentialSubject["id"] as? String
            ?: return Result.Failure(Exception("Invalid credential subject"))
        val subjectDid = DidKeyEd(subjectId)
        val subjectPublicKey = subjectDid.extractPublicKey()
            ?: return Result.Failure(Exception("Invalid credential subject ID"))
        val subjectAccountId = KeyPair.fromPublicKey(subjectPublicKey).accountId
        val subjectPaymentsPath = StellarCredentialStatus.PATH.format(subjectAccountId)

        if ((credentialStatus?.get("type") as? String) != StellarCredentialStatus.TYPE) {
            return Result.Failure(Exception("Invalid credential status"))
        }
        if (subjectPaymentsPath != credentialStatus!!["id"]) {
            return Result.Failure(
                Exception("Credential status does not match credential subject")
            )
        }

        val verifyProofResult = this.verifyProof()
        if (verifyProofResult is SimpleResult.Failure) {
            return Result.Failure(verifyProofResult.error)
        }

        return StellarCredentialStatus.validate(subjectAccountId, credentialStatus!!)
    }

    companion object {
        const val CONTEXT = "https://example.com/credentials/product-cert"
        const val TYPE = "ProductCertificate"
    }
}

class StellarCredentialStatus() {
    companion object {
        const val CONTEXT = "https://example.com/credentials/stellar-status"
        const val TYPE = "StellarCredentialStatus2020"
        const val PATH = "https://horizon.stellar.org/accounts/%s/payments"

        suspend fun validate(
            subjectAccountId: String,
            stellarCredentialStatus: Map<String, Any>
        ): Result<CertificateVerificationResult> {
            val issuerAccountId = stellarCredentialStatus["issuerAccount"] as? String
                ?: return Result.Failure(Exception("InvalidCredentialStatus"))
            val ownershipAccountId = stellarCredentialStatus["ownershipAccount"] as? String
                ?: return Result.Failure(Exception("InvalidCredentialStatus"))
            val revocationAccountId = stellarCredentialStatus["revocationAccount"] as? String
                ?: return Result.Failure(Exception("InvalidCredentialStatus"))

            val networkManager = StellarPaymentsNetworkManager()
            val payments =
                when (val result = networkManager.getPayments(subjectAccountId)) {
                    is Result.Success -> result.data
                    is Result.Failure -> return result
                }

            val issuerPayments = payments.filter { it.sourceAccount == issuerAccountId }
            val ownershipPayments = payments.filter { it.sourceAccount == ownershipAccountId }
            val revocationPayments = payments.filter { it.sourceAccount == revocationAccountId }


            if (issuerPayments.isEmpty()) {
                return Result.Success(CertificateVerificationResult(CertificateState.Invalid))
            }
            if (revocationPayments.isNotEmpty()) {
                return Result.Success(CertificateVerificationResult(CertificateState.Revoked))
            }
            if (ownershipPayments.isNotEmpty()) {
                return Result.Success(CertificateVerificationResult(CertificateState.Claimed)) // TODO: check owner
            }

            return Result.Success(CertificateVerificationResult(CertificateState.New))
        }
    }
}

data class CertificateVerificationResult(
    val state: CertificateState,
    val owner: String? = null
)

enum class CertificateState { Invalid, New, Claimed, Revoked }


