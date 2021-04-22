package com.tangem.id.documents

import com.tangem.Log
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.extensions.calculateSha256
import com.tangem.id.proof.LinkedDataProof
import com.tangem.id.proof.Secp256k1Proof
import com.tangem.id.utils.normalizeJsonLd
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

abstract class VerifiableDocument(
    val context: MutableSet<String>,
    val type: MutableSet<String>,
    var proof: LinkedDataProof?
) {
    abstract fun toJson(): String

    fun toJSONObject() = JSONObject(this.toJson())

    // TODO: check issuer DID for verification method key and valid from date
    suspend fun verify(): SimpleResult {
        val proofType = proof?.type ?: return SimpleResult.Failure(Exception("Proof not found"))
        if (proofType != Secp256k1Proof.TYPE) {
            return SimpleResult.Failure(Exception("Unknown proof type"))
        }

        return (proof as Secp256k1Proof).verify(this)
    }

    suspend fun calculateVerifyHash(
        inputProofOptions: LinkedDataProof? = null
    ): Result<ByteArray> {

        val proofOptions = inputProofOptions?.toJSONObject()
            ?: proof?.toJSONObject()
            ?: return Result.Failure(error("No proof options found"))

        proofOptions.put("@context", JSONArray(context))
        proofOptions.remove("jws")

        val document = this.toJSONObject()
        document.remove("proof")

        Log.debug { "TangemCredential" + this.toJson() }

//        val documentHash = document.toString().calculateSha256()
//        val proofOptionsHash = proofOptions.toString().calculateSha256()
//        return Result.Success(proofOptionsHash + documentHash)


        return try {
            coroutineScope {
                val normalizedDocumentDeferred = async { normalizeJsonLd(document.toString()) }
                val normalizedProofOptionsDeferred = async { normalizeJsonLd(proofOptions.toString()) }

                val documentNormalizationResult = normalizedDocumentDeferred.await()
                val proofOptionsNormalizationResult = normalizedProofOptionsDeferred.await()

                val normalizedDocument = when (documentNormalizationResult) {
                    is Result.Success -> documentNormalizationResult.data
                    is Result.Failure -> return@coroutineScope documentNormalizationResult
                }
                val normalizedProofOptions = when (proofOptionsNormalizationResult) {
                    is Result.Success -> proofOptionsNormalizationResult.data
                    is Result.Failure -> return@coroutineScope proofOptionsNormalizationResult
                }
                val documentHash = normalizedDocument.calculateSha256()
                val proofOptionsHash = normalizedProofOptions.calculateSha256()

                Result.Success(proofOptionsHash + documentHash)
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    companion object {
        const val DEFAULT_CONTEXT = "https://www.w3.org/2018/credentials/v1"
    }
}