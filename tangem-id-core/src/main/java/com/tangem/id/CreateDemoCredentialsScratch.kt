package com.tangem.id

import android.content.Context
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.id.demo.DemoCredentialFactory
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.proof.Secp256k1Proof
import com.tangem.id.utils.JsonLdCborEncoder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun createDemoCredentials(
    issuerWalletManager: EthereumIssuerWalletManager,
    subjectEthereumAddress: String,
    personData: DemoPersonData,
    signer: TransactionSigner,
    androidContext: Context
): SimpleResult {
    val issuer = "did:ethr:${issuerWalletManager.wallet.address}"

    val credentialFactory = DemoCredentialFactory(
        issuer = issuer,
        personData = personData,
        subjectId = "did:ethr:$subjectEthereumAddress",
        androidContext = androidContext
    )

    return coroutineScope {
        val credentials = credentialFactory.createCredentials()

        val hashDeferredList = credentials.map { credential ->
            val proof = Secp256k1Proof("$issuer#owner")
            credential.proof = proof
            async { proof.calculateHashToSign(credential, androidContext) }
        }

        val credentialHashes = hashDeferredList.map { deferred ->
            when (val result = deferred.await()) {
                is Result.Success -> result.data
                is Result.Failure -> return@coroutineScope SimpleResult.Failure(result.error)
            }
        }

        val approvalTransaction =
            when (val result =
                issuerWalletManager.buildTransaction(credentials[0].ethCredentialStatus!!)) {
                is Result.Success -> result.data
                is Result.Failure -> return@coroutineScope SimpleResult.Failure(result.error)
            }

        val arrayToSign = (approvalTransaction.hashes + credentialHashes).toTypedArray()

        val signatures =
            when (val signerResponse = signer.sign(arrayToSign, issuerWalletManager.cardId)) {
                is CompletionResult.Success -> signerResponse.data.signature
                is CompletionResult.Failure -> return@coroutineScope SimpleResult.failure(
                    signerResponse.error
                )
            }
        val transactionSignature = signatures.sliceArray(0 until SIGNATURE_SIZE)

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

        val cborCredentials = credentials.map { credential ->
            JsonLdCborEncoder.encode(credential.toMap())
        }

        //TODO: write cborCredentials to card

        issuerWalletManager.sendSignedTransaction(approvalTransaction, transactionSignature)
    }
}

const val SIGNATURE_SIZE = 64