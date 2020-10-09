package com.tangem.id.features.microsoft

import com.microsoft.did.sdk.crypto.keys.KeyType
import com.microsoft.did.sdk.crypto.keys.PublicKey
import com.microsoft.did.sdk.crypto.keys.ellipticCurve.EllipticCurvePublicKey
import com.microsoft.did.sdk.crypto.models.webCryptoApi.JsonWebKey
import com.microsoft.did.sdk.crypto.models.webCryptoApi.algorithms.EcKeyGenParams
import com.microsoft.did.sdk.identifier.SidetreePayloadProcessor
import com.microsoft.did.sdk.identifier.models.payload.RegistrationPayload
import com.microsoft.did.sdk.util.Base64Url
import com.microsoft.did.sdk.util.Constants
import com.microsoft.did.sdk.util.publicToXY
import com.microsoft.did.sdk.util.serializer.Serializer

object TangemIonIdentifierFactory {

    fun encodePublicKeyJwk(publicKey: ByteArray): EllipticCurvePublicKey {
        val xyData = publicToXY(publicKey)
        val algorithm = EcKeyGenParams("P-256K")
        val kid = "tangem_1"
        val jwk = JsonWebKey(
            kty = KeyType.EllipticCurve.value,
            crv = algorithm.namedCurve,
            x = xyData.first.trim(),
            y = xyData.second.trim()
        )
        return EllipticCurvePublicKey(jwk).apply { this.kid = kid } // TODO: check
    }

    fun encodeIdentifierLongForm(jwkPublicKey: PublicKey): String {
        val payloadProcessor = SidetreePayloadProcessor(Serializer())
        val registrationPayload =
            payloadProcessor.generateCreatePayload(jwkPublicKey, jwkPublicKey, jwkPublicKey)
        val registrationPayloadEncoded =
            registrationPayload.suffixData + "." + registrationPayload.patchData
        val identifierShortForm = computeUniqueSuffix(payloadProcessor, registrationPayload)
        return "$identifierShortForm?${Constants.INITIAL_STATE_LONGFORM}=$registrationPayloadEncoded"
    }

    private fun computeUniqueSuffix(
        payloadProcessor: SidetreePayloadProcessor,
        registrationPayload: RegistrationPayload
    ): String {
        val suffixDataByteArray = Base64Url.decode(registrationPayload.suffixData)
        val suffixDataHash = payloadProcessor.multiHash(suffixDataByteArray)
        val uniqueSuffix = Base64Url.encode(suffixDataHash)
        return "did:${Constants.METHOD_NAME}:$uniqueSuffix"
    }
}