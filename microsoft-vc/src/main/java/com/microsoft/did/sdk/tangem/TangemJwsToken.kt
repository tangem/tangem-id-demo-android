package com.microsoft.did.sdk.tangem

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory
import com.nimbusds.jose.util.Base64URL
import java.security.PublicKey

class TangemJwsToken private constructor(
    private var jwsObject: JWSObject
) {
    private val builder = JWSHeader.Builder(jwsObject.header)

    companion object {
        fun deserialize(jws: String): TangemJwsToken {
            return TangemJwsToken(JWSObject.parse(jws))
        }
    }

    constructor(content: ByteArray) : this(
        JWSObject(
            JWSHeader(JWSAlgorithm.ES256K),
            Payload(content)
        )
    )

    constructor(content: String) : this(
        JWSObject(
            JWSHeader(JWSAlgorithm.ES256K),
            Payload(Base64URL.encode(content))
        )
    )

    fun getKeyId(): String? {
        return jwsObject.header.keyID
    }

    fun setKeyId(string: String) {
        builder.keyID(string)
    }

    fun setType(type: JOSEObjectType) {
        builder.type(type)
    }

    fun setHeader(headerKey: String, headerValue: String) {
        builder.customParam(headerKey, headerValue)
    }

    fun serialize(): String {
        return jwsObject.serialize()
    }

    fun sign(keyManager: TangemKeyManager) {
        jwsObject = JWSObject(builder.build(), jwsObject.payload)
        jwsObject.sign(keyManager)
    }

    fun verify(publicKeys: List<PublicKey> = emptyList()): Boolean {
        for (key in publicKeys) {
            val verifier = DefaultJWSVerifierFactory().createJWSVerifier(jwsObject.header, key)
            if (jwsObject.verify(verifier)) {
                return true
            }
        }
        return false
    }

    /**
     * Plaintext payload content
     */
    fun content(): String {
        return jwsObject.payload.toString()
    }
}