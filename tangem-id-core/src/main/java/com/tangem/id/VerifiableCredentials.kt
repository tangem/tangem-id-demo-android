package com.tangem.id

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import com.tangem.blockchain.extensions.Result

suspend fun createTangemEthCredential(
    issuerId: String,
    credentialContext: Collection<String>,
    credentialType: Collection<String>,
    credentialSubject: Map<String, Any>,
    androidContext: Context
): Result<Map<String, Any>> {

    val credentialContext = credentialContext.toMutableSet()
    credentialContext.add("https://www.w3.org/2018/credentials/v1") //TODO: add tangem credential context

    val credentialType = credentialType.toMutableSet()
    credentialType.add("VerifiableCredential")
    credentialType.add("TangemEthCredential")

    val credentialMap = mutableMapOf(
        "@context" to credentialContext,
        "type" to credentialType,
        "issuer" to issuerId,
        "issuanceDate" to Instant.now().toString(),
        "credentialSubject" to credentialSubject
    )
    val credentialJson = JSONObject(credentialMap as Map<*, *>)

    val normalizationResult = normalizeJsonLd(credentialJson, androidContext)
    val normalizedCredential = when (normalizationResult) {
        is Result.Success -> normalizationResult.data
        is Result.Failure -> return normalizationResult
    }
    val normalizedCredentialHash = normalizedCredential.toByteArray().calculateSha3v512()
    val credentialEthereumAddress =
        EthereumAddressService().makeAddress(ByteArray(1) + normalizedCredentialHash)

    val credentialId = "did:ethr:$credentialEthereumAddress"
    credentialMap.put("id", credentialId)

    return Result.Success(credentialMap)
}

fun createDefaultPresentation(credentials: Collection<Map<String, Any>>): Map<String, Any> = mapOf(
    "@context" to "https://www.w3.org/2018/credentials/v1",
    "type" to setOf("VerifiablePresentation"),
    "verifiableCredential" to credentials
)