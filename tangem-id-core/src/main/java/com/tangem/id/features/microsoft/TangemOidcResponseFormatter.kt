//package com.tangem.id.features.microsoft
//
//import com.microsoft.did.sdk.credential.models.VerifiableCredential
//import com.microsoft.did.sdk.credential.service.RequestedIdTokenMap
//import com.microsoft.did.sdk.credential.service.RequestedSelfAttestedClaimMap
//import com.microsoft.did.sdk.credential.service.RequestedVchMap
//import com.microsoft.did.sdk.credential.service.models.oidc.OidcResponseContent
//import com.microsoft.did.sdk.credential.service.protectors.createIatAndExp
//import com.microsoft.did.sdk.crypto.keys.ellipticCurve.EllipticCurvePublicKey
//import com.microsoft.did.sdk.crypto.protocols.jose.JoseConstants
//import com.microsoft.did.sdk.crypto.protocols.jose.jws.JwsSignature
//import com.microsoft.did.sdk.crypto.protocols.jose.jws.JwsToken
//import com.microsoft.did.sdk.util.Base64Url
//import com.microsoft.did.sdk.util.controlflow.FormatterException
//import com.microsoft.did.sdk.util.controlflow.KeyException
//import com.microsoft.did.sdk.util.serializer.Serializer
//import com.microsoft.did.sdk.util.stringToByteArray
//import com.tangem.blockchain.common.TransactionSigner
//import com.tangem.blockchain.extensions.Result
//import com.tangem.blockchain.extensions.SimpleResult
//import com.tangem.common.CompletionResult
//import com.tangem.common.extensions.calculateSha256
//import org.spongycastle.jcajce.provider.digest.SHA256
//import java.util.*
//
///**
// * Class that forms Response Contents Properly.
// */
//class TangemOidcResponseFormatter(
//    private val serializer: Serializer,
////    private val verifiablePresentationFormatter: VerifiablePresentationFormatter,
//    private val cardId: String,
//    private val publicKey: EllipticCurvePublicKey,
//    private val did: String
//) {
//
//    suspend fun formatAndSign(
//        responseAudience: String,
//        presentationsAudience: String = "",
//        expiryInSeconds: Int,
//        requestedVchMap: RequestedVchMap = mutableMapOf(),
//        requestedIdTokenMap: RequestedIdTokenMap = mutableMapOf(),
//        requestedSelfAttestedClaimMap: RequestedSelfAttestedClaimMap = mutableMapOf(),
//        contract: String? = null,
//        nonce: String? = null,
//        state: String? = null,
//        transformingVerifiableCredential: VerifiableCredential? = null,
//        recipientIdentifier: String? = null,
//        signer: TransactionSigner
//    ): Result<String> {
//        val (iat, exp) = createIatAndExp(expiryInSeconds)
//        if (exp == null) {
//            throw FormatterException("Expiry for OIDC Responses cannot be null")
//        }
//        val jti = UUID.randomUUID().toString()
////        val attestationResponse = this.createAttestationClaimModel(  TODO: implement for presentation
////            requestedVchMap,
////            requestedIdTokenMap,
////            requestedSelfAttestedClaimMap,
////            presentationsAudience,
////            responder
////        )
//        val attestationResponse = null
//
//        val contents = OidcResponseContent(
//            sub = getThumbprint(),
//            aud = responseAudience,
//            nonce = nonce,
//            did = did,
//            subJwk = publicKey.toJWK(),
//            iat = iat,
//            exp = exp,
//            state = state,
//            jti = jti,
//            contract = contract,
//            attestations = attestationResponse,
//            vc = transformingVerifiableCredential?.raw,
//            recipient = recipientIdentifier
//        )
//        return signContents(contents, signer)
//    }
//
//    fun getThumbprint(): String {
//        // construct a JSON object with only required fields
//        val json = publicKey.minimumAlphabeticJwk()
//        val jsonUtf8 = stringToByteArray(json)
//        val hash = jsonUtf8.calculateSha256()
//        // undocumented, but assumed base64url of hash is returned
//        return Base64Url.encode(hash)
//    }
//
//    private suspend fun signContents(contents: OidcResponseContent, signer: TransactionSigner): Result<String> {
//        val payload = serializer.stringify(OidcResponseContent.serializer(), contents)
//        val token = JwsToken(payload, serializer)
//        val additionalHeaders = mutableMapOf<String, String>()
//        additionalHeaders[JoseConstants.Kid.value] = publicKey.kid // TODO: check if needed
//        return when (val result = token.signTangem(signer, additionalHeaders)) {
//            is SimpleResult.Success -> Result.Success(token.serialize(serializer))
//            is SimpleResult.Failure -> Result.Failure(result.error)
//        }
//    }
//
//    private suspend fun JwsToken.signTangem(signer: TransactionSigner, header: Map<String, String> = emptyMap()): SimpleResult {
//        val headers = header.toMutableMap()
//        val protected = mutableMapOf<String, String>()
//
//        var encodedProtected = ""
//        if (protected.isNotEmpty()) {
//            val jsonProtected = serializer.stringify(protected, String::class, String::class)
//            encodedProtected = Base64Url.encode(stringToByteArray(jsonProtected))
//        }
//
//        val signatureInput = stringToByteArray("$encodedProtected.${this.payload}").calculateSha256() //TODO: check
//
//        val signature = when (val result = signer.sign(arrayOf(signatureInput), cardId)) {
//            is CompletionResult.Success -> result.data.signature
//            is CompletionResult.Failure -> return SimpleResult.failure(result.error)
//        }
//
//        val signatureBase64 = Base64Url.encode(signature)
//
//        this.signatures.add(
//            JwsSignature(
//                protected = encodedProtected,
//                header = headers,
//                signature = signatureBase64
//            )
//        )
//
//        return SimpleResult.Success
//    }
//
////    private fun createAttestationClaimModel(
////        requestedVchMap: RequestedVchMap,
////        requestedIdTokenMap: RequestedIdTokenMap,
////        requestedSelfAttestedClaimMap: RequestedSelfAttestedClaimMap,
////        presentationsAudience: String,
////        responder: Identifier
////    ): AttestationClaimModel? {
////        if (areNoCollectedClaims(
////                requestedVchMap,
////                requestedIdTokenMap,
////                requestedSelfAttestedClaimMap
////            )
////        ) {
////            return null
////        }
////        val presentationAttestations = createPresentations(
////            requestedVchMap,
////            presentationsAudience,
////            responder
////        )
////        val nullableSelfAttestedClaimRequestMapping = if (requestedSelfAttestedClaimMap.isEmpty()) {
////            null
////        } else {
////            requestedSelfAttestedClaimMap
////        }
////        val nullableIdTokenRequestMapping = if (requestedIdTokenMap.isEmpty()) {
////            null
////        } else {
////            requestedIdTokenMap
////        }
////        return AttestationClaimModel(
////            nullableSelfAttestedClaimRequestMapping,
////            nullableIdTokenRequestMapping,
////            presentationAttestations
////        )
////    }
//
////    private fun createPresentations(
////        requestedVchMap: RequestedVchMap,
////        audience: String,
////        responder: Identifier
////    ): Map<String, String>? {
////        val vpMap = requestedVchMap.map { (key, value) ->
////            key.credentialType to verifiablePresentationFormatter.createPresentation(
////                value.verifiableCredential,
////                key.validityInterval,
////                audience,
////                responder
////            )
////        }.toMap()
////        return if (vpMap.isEmpty()) {
////            null
////        } else {
////            vpMap
////        }
////    }
//
//    private fun areNoCollectedClaims(
//        requestedVchMap: RequestedVchMap,
//        requestedIdTokenMap: RequestedIdTokenMap,
//        requestedSelfAttestedClaimMap: RequestedSelfAttestedClaimMap
//    ): Boolean {
//        return (requestedVchMap.isNullOrEmpty() && requestedIdTokenMap.isNullOrEmpty() && requestedSelfAttestedClaimMap.isNullOrEmpty())
//    }
//}