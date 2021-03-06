package com.microsoft.did.sdk.tangem

import com.microsoft.did.sdk.credential.service.protectors.createIssuedAndExpiryTime
import com.microsoft.did.sdk.credential.models.VerifiableCredential
import com.microsoft.did.sdk.credential.service.models.verifiablePresentation.VerifiablePresentationContent
import com.microsoft.did.sdk.credential.service.models.verifiablePresentation.VerifiablePresentationDescriptor
import com.microsoft.did.sdk.identifier.models.Identifier
import com.microsoft.did.sdk.util.Constants
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TangemVerifiablePresentationFormatter @Inject constructor(
    private val serializer: Json,
    private val signer: TangemTokenSigner
) {

    // only support one VC per VP
    fun createPresentation(
        verifiableCredential: VerifiableCredential,
        validityInterval: Int,
        audience: String,
        responder: Identifier
    ): String {
        val verifiablePresentation = VerifiablePresentationDescriptor(
            verifiableCredential = listOf(verifiableCredential.raw),
            context = listOf(Constants.VP_CONTEXT_URL),
            type = listOf(Constants.VERIFIABLE_PRESENTATION_TYPE)
        )

        val (issuedTime, expiryTime: Long) = createIssuedAndExpiryTime(validityInterval)
        val vpId = UUID.randomUUID().toString()
        val responderDid = responder.id
        val contents =
            VerifiablePresentationContent(
                vpId = vpId,
                verifiablePresentation = verifiablePresentation,
                issuerOfVp = responderDid,
                tokenIssuedTime = issuedTime,
                tokenNotValidBefore = issuedTime,
                tokenExpiryTime = expiryTime,
                audience = audience
            )
        val serializedContents = serializer.encodeToString(VerifiablePresentationContent.serializer(), contents)
        return signer.signWithIdentifier(serializedContents, responder)
    }
}