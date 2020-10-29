package com.tangem.id.proof

import android.util.Base64
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.documents.VerifiableDocument
import com.tangem.id.proofdemo.DidKeyEd
import org.stellar.sdk.KeyPair

// Supports DID key only for now

@JsonClass(generateAdapter = true)
class Ed25519Proof(
    verificationMethod: String,
    challenge: String? = null
) : LinkedDataProof(TYPE, verificationMethod, challenge) {

    suspend fun calculateDataToSign(
        document: VerifiableDocument
    ): Result<ByteArray> {
        return document.calculateVerifyHash( this)
    }

    suspend fun verify(
        document: VerifiableDocument,
        requiredSignerDid: String? = null
    ): SimpleResult {
        if (!verificationMethod.startsWith(DidKeyEd.DID_KEY_SCHEME)) {
            return SimpleResult.Failure(Exception("Unknown verification method"))
        }
        val verificationMethodSplit = verificationMethod.split("#")
        if (verificationMethodSplit.size != 2) {
            return SimpleResult.Failure(Exception("Unknown verification method"))
        }
        val signerDid = verificationMethodSplit[0]
        val keyId = verificationMethodSplit[1]
        if (signerDid.removePrefix(DidKeyEd.DID_KEY_SCHEME) != keyId) {
            return SimpleResult.Failure(Exception("Unknown verification method"))
        }

        if (requiredSignerDid != null && requiredSignerDid != signerDid) {
            return SimpleResult.Failure(Exception("Invalid signer"))
        }

        val signerPublicKey = DidKeyEd(signerDid).extractPublicKey()
            ?: return SimpleResult.Failure(Exception("Invalid signer DID"))

        val hash =
            when (val result = calculateDataToSign(document)) {
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

        return when (checkEdSignature(decodedSignature, hash, signerPublicKey)) {
            true -> SimpleResult.Success
            false -> SimpleResult.Failure(Exception("Invalid signature"))
        }
    }

    fun addSignature(signature: ByteArray) {
        val encodedHeader = Base64.encodeToString(JWS_HEADER.toByteArray(), Base64.URL_SAFE)
        val encodedSignature = Base64.encodeToString(signature, Base64.URL_SAFE)

        jws = "$encodedHeader..$encodedSignature"
    }

    private fun checkEdSignature(
        signature: ByteArray,
        signedHash: ByteArray,
        publicKey: ByteArray
    ): Boolean {
        return KeyPair.fromPublicKey(publicKey).verify(signedHash, signature)
    }

    companion object {
        const val TYPE = "Ed25519Signature2018"
        const val JWS_HEADER = "{\"alg\":\"EdDSA\",\"b64\":false,\"crit\":[\"b64\"]}"
    }
}