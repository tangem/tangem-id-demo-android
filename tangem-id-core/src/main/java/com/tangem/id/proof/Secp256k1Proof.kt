package com.tangem.id.proof

import android.content.Context
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.calculateSha256
import com.tangem.id.documents.VerifiableDocument
import org.apache.commons.codec.binary.Base64

class Secp256k1Proof(
    verificationMethod: String,
    challenge: String? = null
) : LinkedDataProof(TYPE, verificationMethod, challenge) {

    suspend fun calculateHashToSign(document: VerifiableDocument, androidContext: Context): Result<ByteArray> {
        val verifyHash =
            when (val result = document.calculateVerifyHash(androidContext, this)) {
                is Result.Success -> result.data
                is Result.Failure -> return result
            }
        return Result.Success(verifyHash.calculateSha256())
    }

    fun addSignature(signature: ByteArray) {
        val header = "{\"alg\":\"ES256K\",\"b64\":false,\"crit\":[\"b64\"]}"
        val encodedHeader = Base64.encodeBase64URLSafeString(header.toByteArray())
        val encodedSignature = Base64.encodeBase64URLSafeString(signature)

        jws = "$encodedHeader..$encodedSignature"
    }

    companion object {
        const val TYPE = "EcdsaSecp256k1Signature2019"
    }
}