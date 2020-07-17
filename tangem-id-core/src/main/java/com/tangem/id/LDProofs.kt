package com.tangem.id

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.apache.commons.codec.binary.Base64
import org.json.JSONObject
import java.time.Instant

fun makeVerifiableDocument(
    document: Map<String, Any>,
    proofOptions: Map<String, Any>,
    jwsSignature: String
): Map<String, Any> {

    val proof = proofOptions.toMutableMap()
    proof["jws"] = jwsSignature

    val verifiableDocument = document.toMutableMap()
    verifiableDocument["proof"] = proof

    return verifiableDocument
}

fun createSecp256k1ProofOptions(verificationMethod: String, challenge: String? = null): Map<String, String> {
    val proofOptions = mutableMapOf(
        "type" to "EcdsaSecp256k1Signature2019",
        "created" to Instant.now().toString(),
        "proofPurpose" to "assertionMethod",
        "verificationMethod" to verificationMethod
    )
    if (challenge != null) proofOptions["challenge"] = challenge

    return proofOptions
}

suspend fun calculateJsonLdVerifyHash(
    document: Map<String, Any>,
    proofOptions: Map<String, Any>,
    androidContext: Context
): Result<ByteArray> {

    return try {
        coroutineScope<Result<ByteArray>> {
            val context = document["@context"]
            val proofOptionsWithContext = proofOptions.toMutableMap()
            proofOptionsWithContext["@context"] = context!!

            val documentJson = JSONObject(document)
            val proofOptionsJson = JSONObject(proofOptionsWithContext as Map<*, *>)

            val documentNormalizationDeferred =
                async { normalizeJsonLd(documentJson, androidContext) }
            val proofOptionsNormalizationDeferred =
                async { normalizeJsonLd(proofOptionsJson, androidContext) }

            val documentNormalizationResult = documentNormalizationDeferred.await()
            val proofOptionsNormalizationResult = proofOptionsNormalizationDeferred.await()

            val normalizedDocument = when (documentNormalizationResult) {
                is Result.Success -> documentNormalizationResult.data
                is Result.Failure -> return@coroutineScope documentNormalizationResult
            }
            val normalizedProofOptions = when (proofOptionsNormalizationResult) {
                is Result.Success -> proofOptionsNormalizationResult.data
                is Result.Failure -> return@coroutineScope proofOptionsNormalizationResult
            }
            val documentHash = normalizedDocument.toByteArray().calculateSha256()
            val proofOptionsHash = normalizedProofOptions.toByteArray().calculateSha256()

            Result.Success(proofOptionsHash + documentHash)
        }
    } catch (exception: Exception) {
        Result.Failure(exception)
    }
}

fun encodeSecp256k1JwsSignature(signature: ByteArray): String {
    val header = "{\"alg\":\"ES256K\",\"b64\":false,\"crit\":[\"b64\"]}"
    val encodedHeader = Base64.encodeBase64URLSafeString(header.toByteArray())
    val encodedSignature = Base64.encodeBase64URLSafeString(signature)

    return "$encodedHeader..$encodedSignature"
}
