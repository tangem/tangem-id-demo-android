package com.tangem.id.utils

import com.microsoft.did.sdk.credential.models.VerifiableCredential
import com.microsoft.did.sdk.credential.models.VerifiableCredentialContent
import com.microsoft.did.sdk.crypto.protocols.jose.jws.JwsToken
import com.microsoft.did.sdk.di.SdkModule


fun MSVeriviableCredentialFromString(tokenString: String): VerifiableCredential {
    val jwsToken = JwsToken.deserialize(tokenString)
    val verifiableCredentialContent = SdkModule().defaultJsonSerializer()
        .decodeFromString(VerifiableCredentialContent.serializer(), jwsToken.content())
    return VerifiableCredential(
        verifiableCredentialContent.jti,
        tokenString,
        verifiableCredentialContent
    )
}