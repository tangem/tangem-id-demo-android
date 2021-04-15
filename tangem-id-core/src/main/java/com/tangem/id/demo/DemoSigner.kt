package com.tangem.id.demo

import org.spongycastle.crypto.params.ECPrivateKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner

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