package com.tangem.id.demo

import org.bitcoinj.core.ECKey.CURVE
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.kethereum.extensions.toBigInteger

class DemoSigner {
    fun sign(hashToSign: ByteArray, privateKey: ECPrivateKeyParameters): ByteArray {
        val signer = ECDSASigner()
        signer.init(true, privateKey)
        val signature = signer.generateSignature(hashToSign)

        var rBytes = signature[0].toByteArray()
        var sBytes = signature[1].toByteArray()

        if (rBytes.size == 33 && rBytes[0] == 0.toByte()) {
            rBytes = rBytes.copyOfRange(1, 33)
        }

        if (sBytes.size == 33 && sBytes[0] == 0.toByte()) {
            sBytes = sBytes.copyOfRange(1, 33)
        }

        return rBytes + sBytes
    }
}