package com.tangem.id

import android.content.Context
import com.tangem.TangemSdk
import com.tangem.blockchain.blockchains.ethereum.TransactionToSign
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.id.demo.DemoCredentialFactory
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.proof.Secp256k1Proof
import com.tangem.id.utils.JsonLdCborEncoder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DemoCredentialsManager(
    private val issuerWalletManager: EthereumIssuerWalletManager,
    private val androidContext: Context
) {
    val issuer = "did:ethr:${issuerWalletManager.wallet.address}"

    private var approvalTransaction: TransactionToSign? = null
    private var transactionSignature: ByteArray? = null

    suspend fun createDemoCredentials(
        personData: DemoPersonData, subjectEthereumAddress: String, signer: TransactionSigner
    ): Result<List<VerifiableCredential>> {

        val credentialFactory = DemoCredentialFactory(
            issuer = issuer,
            personData = personData,
            subjectId = "did:ethr:$subjectEthereumAddress",
            androidContext = androidContext
        )

        val credentials = credentialFactory.createCredentials().drop(0)

        val credentialHashes = credentials
            .map { credential ->
                val proof = Secp256k1Proof("$issuer#owner")
                credential.proof = proof
                proof.calculateHashToSign(credential, androidContext)
            }
            .map { result ->
                when (result) {
                    is Result.Success -> result.data
                    is Result.Failure -> return Result.Failure(result.error)
                }
            }

        val approvalTransaction =
            when (val result =
                issuerWalletManager.buildTransaction(credentials[0].ethCredentialStatus!!)) {
                is Result.Success -> result.data
                is Result.Failure -> return Result.Failure(result.error)
            }

        val arrayToSign = (approvalTransaction.hashes + credentialHashes).toTypedArray()

        val signatures =
            when (val signerResponse = signer.sign(arrayToSign, issuerWalletManager.cardId)) {
                is CompletionResult.Success -> signerResponse.data.signature
                is CompletionResult.Failure -> return Result.Failure(
                    Exception(
                        "TangemError: code: ${signerResponse.error.code}, " +
                                "message: ${signerResponse.error.customMessage}"
                    )
                )
            }
        transactionSignature = signatures.sliceArray(0 until SIGNATURE_SIZE)

        val credentialSignatures = mutableListOf<ByteArray>()
        for (index in 1..4) {
            credentialSignatures.add(
                signatures.copyOfRange(index * SIGNATURE_SIZE, (index + 1) * SIGNATURE_SIZE)
            )
        }

        for (index in credentials.indices) {
            val credential = credentials[index]
            val secp256k1Proof = credential.proof as Secp256k1Proof
            secp256k1Proof.addSignature(credentialSignatures[index])
        }

        return Result.Success(credentials)

    }

    suspend fun completeWithId(
        credentials: List<VerifiableCredential>,
        tangemSdk: TangemSdk
    ): SimpleResult {

        if (approvalTransaction == null || transactionSignature == null) return SimpleResult.Failure(
            Error()
        )

        val cborCredentials = credentials.map { credential ->
            JsonLdCborEncoder.encode(credential.toMap())
        }

        val result = suspendCancellableCoroutine<SimpleResult> { continuation ->
            tangemSdk.startSessionWithRunnable(
                WriteFilesTask(cborCredentials, issuer().dataKeyPair)
            ) { result ->
                when (result) {
                    is CompletionResult.Failure -> if (continuation.isActive) {
                        continuation.resume(SimpleResult.failure(result.error))
                    }
                    is CompletionResult.Success -> {
                        if (continuation.isActive) continuation.resume(SimpleResult.Success)
                    }
                }
            }
        }
        return when (result) {
            SimpleResult.Success -> issuerWalletManager.sendSignedTransaction(
                approvalTransaction!!,
                transactionSignature!!
            )
            is SimpleResult.Failure -> result
        }
    }

    companion object {
        const val SIGNATURE_SIZE = 64
    }
}
