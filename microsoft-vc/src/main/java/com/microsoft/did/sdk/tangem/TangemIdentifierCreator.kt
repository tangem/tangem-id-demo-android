package com.microsoft.did.sdk.tangem

import android.util.Base64
import com.microsoft.did.sdk.identifier.SideTreeHelper
import com.microsoft.did.sdk.identifier.SidetreePayloadProcessor
import com.microsoft.did.sdk.identifier.models.Identifier
import com.microsoft.did.sdk.identifier.models.payload.RegistrationPayload
import com.microsoft.did.sdk.identifier.models.payload.SuffixData
import com.microsoft.did.sdk.util.Constants
import com.nimbusds.jose.jwk.JWK
import kotlinx.serialization.json.Json
import org.erdtman.jcs.JsonCanonicalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TangemIdentifierCreator @Inject constructor(
    private val payloadProcessor: SidetreePayloadProcessor,
    private val sideTreeHelper: SideTreeHelper,
    private val serializer: Json
) {

    fun createIdentifier(
        personaName: String,
        signingPublicKey: JWK,
        recoveryPublicKey: JWK,
        updatePublicKey: JWK
    ): Identifier {
        val registrationPayload = payloadProcessor.generateCreatePayload(
            signingPublicKey,
            recoveryPublicKey,
            updatePublicKey
        )
        val identifierLongForm = computeLongFormIdentifier(registrationPayload)

        return Identifier(
            identifierLongForm,
            signingPublicKey.keyID,
            "",
            recoveryPublicKey.keyID,
            updatePublicKey.keyID,
            personaName
        )
    }

    private fun computeDidShortFormIdentifier(registrationPayload: RegistrationPayload): String {
        val suffixDataString =
            serializer.encodeToString(SuffixData.serializer(), registrationPayload.suffixData)
        val uniqueSuffix = sideTreeHelper.canonicalizeMultiHashEncode(suffixDataString)
        return "did${Constants.COLON}${Constants.METHOD_NAME}${Constants.COLON}$uniqueSuffix"
    }

    private fun computeLongFormIdentifier(registrationPayload: RegistrationPayload): String {
        val registrationPayloadString =
            serializer.encodeToString(RegistrationPayload.serializer(), registrationPayload)
        val registrationPayloadCanonicalized =
            JsonCanonicalizer(registrationPayloadString).encodedUTF8
        val registrationPayloadCanonicalizedEncoded =
            Base64.encodeToString(registrationPayloadCanonicalized, Constants.BASE64_URL_SAFE)
        val identifierShortForm = computeDidShortFormIdentifier(registrationPayload)
        return "$identifierShortForm${Constants.COLON}$registrationPayloadCanonicalizedEncoded"
    }
}