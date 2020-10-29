package com.tangem.id.proof

import android.content.Context
import android.util.Base64
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.extensions.calculateSha256
import com.tangem.id.documents.VerifiableDocument
import org.bitcoinj.core.ECKey
import org.kethereum.crypto.CryptoAPI
import org.kethereum.crypto.api.ec.ECDSASignature
import java.math.BigInteger

@JsonClass(generateAdapter = true)
class Secp256k1Proof(
    verificationMethod: String,
    challenge: String? = null
) : LinkedDataProof(TYPE, verificationMethod, challenge) {

    suspend fun calculateHashToSign(
        document: VerifiableDocument
    ): Result<ByteArray> {
        val verifyHash =
            when (val result = document.calculateVerifyHash( this)) {
                is Result.Success -> result.data
                is Result.Failure -> return result
            }
        return Result.Success(verifyHash.calculateSha256())
    }

    suspend fun verify(
        document: VerifiableDocument
    ): SimpleResult {
        if (!verificationMethod.startsWith("did:ethr:")) {
            return SimpleResult.Failure(Exception("Unknown verification method"))
        }
        val issuerEthereumAddress =
            verificationMethod.removePrefix("did:ethr:").removeSuffix("#owner")

        val hash =
            when (val result = calculateHashToSign(document)) {
                is Result.Success -> result.data
                is Result.Failure -> return SimpleResult.Failure(result.error)
            }

        val jwsParts = jws?.split("..")
            ?: return SimpleResult.Failure(Exception("Signature not found"))
        val jwsHeader = jwsParts[0]
        val jwsSignature = jwsParts[1]

        val decodedHeader = String(Base64.decode(jwsHeader, Base64.URL_SAFE))
        if (decodedHeader != JWS_HEADER) {
            return SimpleResult.Failure(Exception("Invalid JWS header"))
        }

        val decodedSignature = Base64.decode(jwsSignature, Base64.URL_SAFE)

        return when (checkEthSignature(decodedSignature, hash, issuerEthereumAddress)) {
            true -> SimpleResult.Success
            false -> SimpleResult.Failure(Exception("Invalid signature"))
        }
    }

    fun addSignature(signature: ByteArray) {
        val rBytes = signature.copyOfRange(0, 32)
        val r = BigInteger(1, rBytes)
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val canonicalSignature = rBytes + canonicalS.toByteArray()

        val encodedHeader = Base64.encodeToString(JWS_HEADER.toByteArray(), Base64.URL_SAFE)
        val encodedSignature = Base64.encodeToString(canonicalSignature, Base64.URL_SAFE)

        jws = "$encodedHeader..$encodedSignature"
    }

    private fun checkEthSignature(signature: ByteArray, signedHash: ByteArray, signerAddress: String): Boolean{
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val ecdsaSignature = ECDSASignature(r, s)

        val signer = CryptoAPI.signer
        for (i in 0..3) {
            val recoveredPublicKey = signer.recover(i, ecdsaSignature, signedHash)?.toByteArray() ?: continue
            val recoveredEthereumAddress = EthereumAddressService().makeAddress(recoveredPublicKey)

            if (recoveredEthereumAddress == signerAddress) {
                return true
            }
        }
        return false
    }

    companion object {
        const val TYPE = "EcdsaSecp256k1Signature2019"
        const val JWS_HEADER = "{\"alg\":\"ES256K\",\"b64\":false,\"crit\":[\"b64\"]}"
    }
}