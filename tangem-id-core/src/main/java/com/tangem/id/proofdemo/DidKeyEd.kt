package com.tangem.id.proofdemo

import org.bitcoinj.core.Base58

class DidKeyEd(val did: String) {

    fun extractPublicKey(): ByteArray? {
        if (!did.startsWith(DID_KEY_SCHEME)) return null
        val multibaseString = did.removePrefix(DID_KEY_SCHEME)

        if (!multibaseString.startsWith(MULTIBASE_BASE_58_BTC_PREFIX)) return null
        val multicodecPublicKey = Base58.decode(
            multibaseString.removePrefix(MULTIBASE_BASE_58_BTC_PREFIX)
        )

        if (multicodecPublicKey[0] != MULTICODEC_ED_PUBLIC_KEY_BYTE
            || multicodecPublicKey.size != ED_PUBLIC_KEY_LENGTH + 1
        ) {
            return null
        }
        return multicodecPublicKey.copyOfRange(1, ED_PUBLIC_KEY_LENGTH + 1)
    }

    fun getVerificationMethod() = did + did.removePrefix(DID_KEY_SCHEME)

    fun isValid() = extractPublicKey() != null

    override fun toString() = did

    companion object {
        fun fromPublicKey(publicKey: ByteArray): DidKeyEd? {
            if (publicKey.size != ED_PUBLIC_KEY_LENGTH) return null
            val multicodecPublicKey = byteArrayOf(MULTICODEC_ED_PUBLIC_KEY_BYTE) + publicKey
            val multibaseString = MULTIBASE_BASE_58_BTC_PREFIX + Base58.encode(multicodecPublicKey)
            return DidKeyEd("DID_KEY_SCHEME$multibaseString")
        }

        const val DID_KEY_SCHEME = "did:key:"
        private const val MULTIBASE_BASE_58_BTC_PREFIX = "z"
        private const val MULTICODEC_ED_PUBLIC_KEY_BYTE = 0xed.toByte()
        private const val ED_PUBLIC_KEY_LENGTH = 32
    }
}