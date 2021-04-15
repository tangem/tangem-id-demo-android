package com.microsoft.did.sdk.tangem

import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.jwk.JWK

interface TangemKeyManager : JWSSigner {
    val publicKey: JWK
}