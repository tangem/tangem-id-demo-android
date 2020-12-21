package com.tangem.id.demo

import android.content.Context
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.extensions.Result
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.proof.Secp256k1Proof
import org.bitcoinj.core.ECKey.CURVE
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.security.SecureRandom

class DemoCovidCredential {
    companion object {
        suspend fun createCovidCredential(subjectEthereumAddress: String): Result<VerifiableCredential> {
            val keyPair = generateKeyPair()
            val privateKey = keyPair.private as ECPrivateKeyParameters
            val publicKey = keyPair.public as ECPublicKeyParameters

            val issuerEthAddress = EthereumAddressService().makeAddress(publicKey.q.getEncoded(false))
            val issuer = "did:ethr:$issuerEthAddress"

            val credentialSubject = mapOf(
                "id" to "did:ethr:$subjectEthereumAddress",
                "result" to "negative"
            )
            val credential = VerifiableCredential(
                credentialSubject = credentialSubject,
                issuer = issuer,
//            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
                extraTypes = setOf(
                    TANGEM_COVID_CREDENTIAL
                )
            )

            val proof = Secp256k1Proof("$issuer#owner")
            credential.proof = proof

            val hashToSign =
                when (val result = proof.calculateHashToSign(credential)) {
                    is Result.Success -> result.data
                    is Result.Failure -> return result
                }
            val signature = DemoSigner().sign(hashToSign, privateKey)

            proof.addSignature(signature)

            return Result.Success(credential)
        }

        private fun generateKeyPair(): AsymmetricCipherKeyPair {
            val gen = ECKeyPairGenerator()
            val secureRandom = SecureRandom()
            val keyGenParam = ECKeyGenerationParameters(CURVE, secureRandom)
            gen.init(keyGenParam)
            return gen.generateKeyPair()
        }

        const val TANGEM_COVID_CREDENTIAL = "TangemCovidCredential"
    }
}