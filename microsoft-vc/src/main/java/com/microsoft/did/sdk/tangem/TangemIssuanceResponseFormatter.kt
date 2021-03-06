package com.microsoft.did.sdk.tangem

import com.microsoft.did.sdk.credential.service.IssuanceResponse
import com.microsoft.did.sdk.credential.service.RequestedIdTokenMap
import com.microsoft.did.sdk.credential.service.RequestedSelfAttestedClaimMap
import com.microsoft.did.sdk.credential.service.RequestedVcMap
import com.microsoft.did.sdk.credential.service.models.oidc.AttestationClaimModel
import com.microsoft.did.sdk.credential.service.models.oidc.IssuanceResponseClaims
import com.microsoft.did.sdk.credential.service.protectors.createIssuedAndExpiryTime
import com.microsoft.did.sdk.identifier.models.Identifier
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TangemIssuanceResponseFormatter @Inject constructor(
    private val serializer: Json,
    private val verifiablePresentationFormatter: TangemVerifiablePresentationFormatter,
    private val signer: TangemTokenSigner,
    private val keyManager: TangemKeyManager
) {

    fun formatResponse(
        requestedVcMap: RequestedVcMap = mutableMapOf(),
        issuanceResponse: IssuanceResponse,
        responder: Identifier,
        expiryInSeconds: Int
    ): String {
        val (issuedTime, expiryTime) = createIssuedAndExpiryTime(expiryInSeconds)
        val responseId = UUID.randomUUID().toString()
        val attestationResponse = this.createAttestationClaimModel(
            requestedVcMap,
            issuanceResponse.requestedIdTokenMap,
            issuanceResponse.requestedSelfAttestedClaimMap,
            issuanceResponse.request.entityIdentifier,
            responder
        )
        return createAndSignOidcResponseContent(
            issuanceResponse,
            responder,
            issuedTime,
            expiryTime,
            responseId,
            attestationResponse
        )
    }

    private fun createAndSignOidcResponseContent(
        issuanceResponse: IssuanceResponse,
        responder: Identifier,
        issuedTime: Long,
        expiryTime: Long,
        responseId: String,
        attestationResponse: AttestationClaimModel
    ): String {
        val publicKey = keyManager.publicKey
        val contents = IssuanceResponseClaims(
            issuanceResponse.request.contractUrl,
            attestationResponse
        ).apply {
            publicKeyThumbPrint = publicKey.computeThumbprint().toString()
            audience = issuanceResponse.audience
            did = responder.id
            publicKeyJwk = publicKey.toPublicJWK()
            responseCreationTime = issuedTime
            responseExpirationTime = expiryTime
            this.responseId = responseId
        }
        return signContents(contents, responder)
    }

    private fun signContents(contents: IssuanceResponseClaims, responder: Identifier): String {
        val serializedResponseContent =
            serializer.encodeToString(IssuanceResponseClaims.serializer(), contents)
        return signer.signWithIdentifier(serializedResponseContent, responder)
    }

    private fun createAttestationClaimModel(
        requestedVcMap: RequestedVcMap,
        requestedIdTokenMap: RequestedIdTokenMap,
        requestedSelfAttestedClaimMap: RequestedSelfAttestedClaimMap,
        presentationsAudience: String,
        responder: Identifier
    ): AttestationClaimModel {
        if (areNoCollectedClaims(
                requestedVcMap,
                requestedIdTokenMap,
                requestedSelfAttestedClaimMap
            )
        ) {
            return AttestationClaimModel()
        }
        val presentationAttestations =
            createPresentations(requestedVcMap, presentationsAudience, responder)
        return AttestationClaimModel(
            requestedSelfAttestedClaimMap,
            requestedIdTokenMap,
            presentationAttestations
        )
    }

    private fun createPresentations(
        requestedVcMap: RequestedVcMap,
        audience: String,
        responder: Identifier
    ): Map<String, String> {
        return requestedVcMap.map { (inputDescriptor, vc) ->
            inputDescriptor.credentialType to verifiablePresentationFormatter.createPresentation(
                vc,
                inputDescriptor.validityInterval,
                audience,
                responder
            )
        }.toMap()
    }

    private fun areNoCollectedClaims(
        requestedVcMap: RequestedVcMap,
        requestedIdTokenMap: RequestedIdTokenMap,
        requestedSelfAttestedClaimMap: RequestedSelfAttestedClaimMap
    ): Boolean {
        return (requestedVcMap.isNullOrEmpty() && requestedIdTokenMap.isNullOrEmpty() && requestedSelfAttestedClaimMap.isNullOrEmpty())
    }
}