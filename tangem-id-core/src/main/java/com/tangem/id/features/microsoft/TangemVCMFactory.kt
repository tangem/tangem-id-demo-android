//package com.tangem.id.features.microsoft
//
//import com.microsoft.did.sdk.util.serializer.Serializer
//import com.tangem.blockchain.common.TransactionSigner
//
//object TangemVCMFactory {
//
//    fun create(cardId: String, publicKey: ByteArray): TangemVerifiableCredentialManager {
//        val serializer = Serializer()
//        val jwk = TangemIonIdentifierFactory.encodePublicKeyJwk(publicKey)
//        val did = TangemIonIdentifierFactory.encodeIdentifierLongForm(jwk)
//        val oidcResponseFormatter = TangemOidcResponseFormatter(serializer, cardId, jwk, did)
//        return TangemVerifiableCredentialManager(oidcResponseFormatter, serializer)
//    }
//}