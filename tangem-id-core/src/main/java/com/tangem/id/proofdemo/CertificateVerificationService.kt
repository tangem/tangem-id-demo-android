package com.tangem.id.proofdemo

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.documents.VerifiableCredential

object CertificateVerificationService {
    suspend fun verify(credential: VerifiableCredential): SimpleResult {
        if (!credential.type.contains(CertificateCredential.TYPE)) {
            return SimpleResult.Failure(Exception("Invalid credential type"))
        }
        val certificateCredential = credential as CertificateCredential
        val verificationResult =
            when (val result = certificateCredential.verify()) {
                is Result.Success -> result.data
                is Result.Failure -> return SimpleResult.Failure(result.error)
            }

        return when (verificationResult.state) {
            CertificateState.Invalid -> SimpleResult.Failure(Exception("Invalid certificate"))
            CertificateState.New -> SimpleResult.Success
            CertificateState.Claimed -> SimpleResult.Failure(Exception("Certificate claimed"))
            CertificateState.Revoked -> SimpleResult.Failure(Exception("Certificate revoked"))
        }
    }
}