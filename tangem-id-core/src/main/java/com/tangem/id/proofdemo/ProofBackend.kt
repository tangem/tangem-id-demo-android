package com.tangem.id.proofdemo

import com.tangem.blockchain.blockchains.stellar.StellarNetworkManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.id.proof.Ed25519Proof
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.stellar.sdk.*
import java.time.Instant
import java.util.*

class ProofBackend {
    val networkManager = StellarNetworkManager()
    val decimals = 7

    private val issuerSeed = "1925B3BF56DA8E8A469C383D939CFE4798B5864DE7BAEAA15F0EC80F36B76B8F"
    private val ownershipSeed = "869E6030A3B42D11ED24AC0D48E8BC54B0911E15092FB5D6214113DA703F7A0C"
    private val revocationSeed = "E7876063042424F9AC220802B8220D34A50DF2CC4E4FC0995DF057BFA93C0ECE"

    private val issuerKey = KeyPair.fromSecretSeed(issuerSeed.hexToBytes())
    private val ownershipKey = KeyPair.fromSecretSeed(ownershipSeed.hexToBytes())
    private val revocatonKey = KeyPair.fromSecretSeed(revocationSeed.hexToBytes())

    private val issuerDidKey = DidKeyEd.fromPublicKey(issuerKey.publicKey)
        ?: throw Exception("Invalid public key length")

    suspend fun issueCertificate(
        itemEdPublicKey: ByteArray
    ): Result<CertificateCredential> {
        return coroutineScope {
            val itemDid = DidKeyEd.fromPublicKey(itemEdPublicKey)
                ?: return@coroutineScope Result.Failure(Exception("Invalid public key length"))
            val itemAccountId = KeyPair.fromPublicKey(itemEdPublicKey).accountId

            val isTargetItemCreatedDeferred =
                async { networkManager.checkIsAccountCreated(itemAccountId) }
            val issuerAccountDeferred = async { networkManager.getInfo(issuerKey.accountId) }

            val operation =
                if (isTargetItemCreatedDeferred.await()) {
                    PaymentOperation.Builder(
                        itemAccountId,
                        AssetTypeNative(),
                        "0.0000001"
                    ).build()
                } else {
                    CreateAccountOperation.Builder(
                        itemAccountId,
                        "1"
                    ).build()
                }

            val issuerAccountData = when (val result = issuerAccountDeferred.await()) {
                is Result.Success -> result.data
                is Result.Failure -> return@coroutineScope Result.Failure(result.error)
            }

            val currentTime = Calendar.getInstance().timeInMillis / 1000
            val minTime = 0L
            val maxTime = currentTime + 120

            val transaction = Transaction.Builder(
                Account(issuerKey.accountId, issuerAccountData.sequence), networkManager.network
            )
                .addOperation(operation)
                .addTimeBounds(TimeBounds(minTime, maxTime))
                .setBaseFee(issuerAccountData.baseFee.movePointRight(decimals).toInt())
                .addMemo(Memo.text("Issued by Demo Issuer"))
                .build()

            transaction.sign(issuerKey)

            val sendResult = networkManager.sendTransaction(transaction.toEnvelopeXdrBase64())
            if (sendResult is SimpleResult.Failure) {
                return@coroutineScope Result.Failure(sendResult.error)
            }

            val certificateCredentialSubject = mapOf(
                "id" to itemDid.did,
                "itemType" to "Demo Type",
                "itemName" to "Demo Item",
                "modelName" to "Demo Model",
                "manufDate" to Instant.now().toString(),
                "itemImage" to "https://example.com/products/demo_item.jpg"
            )

            val stellarCredentialStatus = mapOf(
                "id" to StellarCredentialStatus.PATH.format(itemAccountId),
                "type" to StellarCredentialStatus.TYPE,
                "issuerAccount" to issuerKey.accountId,
                "ownershipAccount" to ownershipKey.accountId,
                "revocationAccount" to revocatonKey.accountId
            )

            val certificateCredential = CertificateCredential(
                credentialSubject = certificateCredentialSubject,
                issuer = issuerDidKey.did,
                credentialStatus = stellarCredentialStatus,
                extraContexts = listOf(StellarCredentialStatus.CONTEXT)
            )

            val proof = Ed25519Proof(issuerDidKey.getVerificationMethod())
            val dataToSign =
                when (val result = proof.calculateDataToSign(certificateCredential)) {
                    is Result.Success -> result.data
                    is Result.Failure -> return@coroutineScope Result.Failure(result.error)
                }
            val signature = issuerKey.sign(dataToSign)
            proof.addSignature(signature)
            certificateCredential.proof = proof

            Result.Success(certificateCredential)
        }
    }
}