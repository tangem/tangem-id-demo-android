package com.microsoft.did.sdk.tangem

import com.microsoft.did.sdk.identifier.models.Identifier
import com.nimbusds.jose.JOSEObjectType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TangemTokenSigner @Inject constructor(
    private val keyManager: TangemKeyManager
) {
    fun signWithIdentifier(payload: String, identifier: Identifier): String {
        val token = TangemJwsToken(payload)
        // adding kid value to header.
        token.setKeyId("${identifier.id}#${identifier.signatureKeyReference}")
        token.setType(JOSEObjectType.JWT)
        token.sign(keyManager)
        return token.serialize()
    }
}