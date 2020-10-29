package com.tangem.id.documents

import android.content.Context
import com.tangem.Log
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.calculateSha512
import com.tangem.id.proof.Ed25519Proof
import com.tangem.id.proof.LinkedDataProof
import com.tangem.id.proof.Secp256k1Proof
import org.bouncycastle.crypto.tls.HashAlgorithm
import org.json.JSONArray
import org.json.JSONObject

abstract class VerifiableDocument(
    val context: MutableSet<String>,
    val type: MutableSet<String>,
    var proof: LinkedDataProof?
) {
    abstract fun toJson(): String

    fun toJSONObject() = JSONObject(this.toJson())

    suspend fun verifyProof(requiredSignerDid: String? = null): SimpleResult {
        val proofType = proof?.type ?: return SimpleResult.Failure(Exception("Proof not found"))

        return when (proofType) {
            Secp256k1Proof.TYPE -> (proof as Secp256k1Proof).verify(this) // TODO: check issuer DID for verification method key
            Ed25519Proof.TYPE -> {
                (proof as Ed25519Proof).verify(this, requiredSignerDid)
            }
            else -> SimpleResult.Failure(Exception("Unknown proof type"))
        }
    }

    suspend fun calculateVerifyHash(
        inputProofOptions: LinkedDataProof? = null,
        hashAlgorithm: ProofHashAlgorithm = ProofHashAlgorithm.Sha256
    ): Result<ByteArray> {

        val proofOptions = inputProofOptions?.toJSONObject()
            ?: proof?.toJSONObject()
            ?: return Result.Failure(error("No proof options found"))

        proofOptions.put("@context", JSONArray(context))
        proofOptions.remove("jws")
//        proofOptions.remove("type") TODO: check https://github.com/w3c-ccg/ld-proofs/issues/27
//        proofOptions.remove("proofPurpose")

        val document = this.toJSONObject()
        document.remove("proof")

        Log.i("TangemCredential", this.toJson())


        val documentHash = hashAlgorithm.calculateHash(document.toString())
        val proofOptionsHash = hashAlgorithm.calculateHash(proofOptions.toString())
        return Result.Success(proofOptionsHash + documentHash)


//        return try {
//            coroutineScope<Result<ByteArray>> {
//                val documentNormalizationDeferred =
//                    async {
//                        normalizeJsonLd(
//                            proofOptions,
//                            androidContext
//                        )
//                    }
//
//
//                val proofOptionsNormalizationDeferred =
//                    async {
//                        normalizeJsonLd(
//                            proofOptions,
//                            androidContext
//                        )
//                    }
//
//                val documentNormalizationResult = documentNormalizationDeferred.await()
//                val proofOptionsNormalizationResult = proofOptionsNormalizationDeferred.await()
//
//                val normalizedDocument = when (documentNormalizationResult) {
//                    is Result.Success -> documentNormalizationResult.data
//                    is Result.Failure -> return@coroutineScope documentNormalizationResult
//                }
//                val normalizedProofOptions = when (proofOptionsNormalizationResult) {
//                    is Result.Success -> proofOptionsNormalizationResult.data
//                    is Result.Failure -> return@coroutineScope proofOptionsNormalizationResult
//                }
//                val documentHash = normalizedDocument.toByteArray().calculateSha256()
//                val proofOptionsHash = normalizedProofOptions.toByteArray().calculateSha256()

//                 Result.Success(proofOptionsHash + documentHash)
//            }
//        } catch (exception: Exception) {
//            Result.Failure(exception)
//        }
    }

    companion object {
        const val DEFAULT_CONTEXT = "https://www.w3.org/2018/credentials/v1"
    }
}

enum class ProofHashAlgorithm {
    Sha256, Sha512;
    fun calculateHash(data: String) = when (this) {
        Sha256 -> data.calculateSha256()
        Sha512 -> data.calculateSha512()
    }
}