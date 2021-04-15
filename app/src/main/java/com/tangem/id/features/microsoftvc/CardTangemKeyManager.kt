package com.tangem.id.features.microsoftvc

import com.microsoft.did.sdk.tangem.TangemKeyManager
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import com.ripple.crypto.ecdsa.ECDSASignature
import com.tangem.blockchain.extensions.Signer
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.calculateSha256
import kotlinx.coroutines.*
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger
import java.security.KeyFactory
import java.security.Security
import java.security.interfaces.ECPublicKey

class CardTangemKeyManager(
    publicKey: ByteArray,
    private val cardId: String,
    private val signer: Signer,
    private val scope: CoroutineScope
) : TangemKeyManager {
    private var signature: ByteArray? = null

    override val publicKey: JWK = kotlin.run {
        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val publicKeyPoint = spec.curve.decodePoint(publicKey)
        Security.addProvider(BouncyCastleProvider())
        val kf = KeyFactory.getInstance("ECDSA", "SC")
        val pubSpec = ECPublicKeySpec(publicKeyPoint, spec)
        val ecPublicKey = kf.generatePublic(pubSpec) as ECPublicKey
        return@run ECKey.Builder(Curve.SECP256K1, ecPublicKey).keyID(cardId).build()
    }

    override fun getJCAContext(): JCAContext {
        return JCAContext()
    }

    override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> {
        return mutableSetOf(JWSAlgorithm.ES256K)
    }

    override fun sign(header: JWSHeader?, signingInput: ByteArray?): Base64URL {
        val signature = runBlocking {
            when (val result = signer.sign(arrayOf(signingInput!!.calculateSha256()), cardId)) {
                is CompletionResult.Success -> result.data.signature
                is CompletionResult.Failure -> {
                    throw Exception(result.error.customMessage)
                }
            }
        }
        val rsByteArrayLength = ECDSA.getSignatureByteArrayLength(
            header!!.algorithm
        )
        val derSignature = encodeDerSignature(signature)

        val jwsSignature = ECDSA.transcodeSignatureToConcat(derSignature, rsByteArrayLength)
        return Base64URL.encode(jwsSignature)

    }

    private fun encodeDerSignature(signature: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        return org.bitcoinj.core.ECKey.ECDSASignature(r, s).toCanonicalised().encodeToDER()
    }

}