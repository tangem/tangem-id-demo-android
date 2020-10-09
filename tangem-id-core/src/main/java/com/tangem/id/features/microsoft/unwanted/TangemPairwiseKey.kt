package com.tangem.id.features.microsoft.unwanted

import com.microsoft.did.sdk.crypto.CryptoOperations
import com.microsoft.did.sdk.crypto.keys.KeyType
import com.microsoft.did.sdk.crypto.keys.PrivateKey
import com.microsoft.did.sdk.crypto.keys.ellipticCurve.EllipticCurvePrivateKey
import com.microsoft.did.sdk.crypto.models.webCryptoApi.JsonWebKey
import com.microsoft.did.sdk.crypto.models.webCryptoApi.algorithms.Algorithm
import com.microsoft.did.sdk.crypto.models.webCryptoApi.algorithms.EcKeyGenParams
import com.microsoft.did.sdk.util.*
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import java.nio.ByteBuffer

class TangemPairwiseKey {

    suspend fun generate(
        crypto: CryptoOperations,
        masterKey: ByteArray,
        peerId: String,
        signer: TransactionSigner,
        cardId: String
    ): Result<PrivateKey> {
        val algorithm = Algorithm("P-256K")

        val pairwiseKeySeedSigned =
            when (val result = generatePairwiseSeed(peerId, signer, cardId)) {
                is Result.Success -> result.data
                is Result.Failure -> return result
            }
        val pairwiseKeySeedUnsigned = reduceKeySeedSizeAndConvertToUnsigned(pairwiseKeySeedSigned)

        val pubKey = generatePublicKeyFromPrivateKey(pairwiseKeySeedUnsigned)
        val xyData = publicToXY(pubKey)

        val pairwiseKeySeedInBigEndian = convertToBigEndian(pairwiseKeySeedUnsigned)

        val pairwiseKey =
            createPairwiseKeyFromPairwiseSeed(algorithm, pairwiseKeySeedInBigEndian, xyData)

        return Result.Success(pairwiseKey)
    }

    private suspend fun generatePairwiseSeed(
        peerId: String,
        signer: TransactionSigner,
        cardId: String
    ): Result<ByteArray> {

        val bytesToSign = arrayOf(peerId.map { it.toByte() }.toByteArray())
        return when (val result = signer.sign(bytesToSign, cardId)) {
            is CompletionResult.Success -> Result.Success(result.data.signature)
            is CompletionResult.Failure -> Result.Failure(
                Exception(
                    "TangemError: code: ${result.error.code}, message: ${result.error.customMessage}"
                )
            )
        }
    }

    private fun createPairwiseKeyFromPairwiseSeed(
        algorithm: Algorithm,
        pairwiseKeySeedInBigEndian: ByteBuffer,
        xyData: Pair<String, String>
    ): PrivateKey {
        val pairwiseKey =
            JsonWebKey(
                kty = KeyType.EllipticCurve.value,
                crv = (algorithm as EcKeyGenParams).namedCurve,
                d = Base64Url.encode(pairwiseKeySeedInBigEndian.array()),
                x = xyData.first.trim(),
                y = xyData.second.trim()
            )
        return EllipticCurvePrivateKey(pairwiseKey)
    }
}