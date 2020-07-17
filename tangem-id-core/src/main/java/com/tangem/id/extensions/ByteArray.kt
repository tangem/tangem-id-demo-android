package com.tangem.id.extensions

import org.spongycastle.crypto.util.DigestFactory

fun ByteArray.calculateSha3v512(): ByteArray {
    val digest = DigestFactory.createSHA3_512()
    digest.update(this, 0, this.size)
    val output = ByteArray(64)
    digest.doFinal(output, 0)
    return output
}

fun ByteArray.calculateSha3v256(): ByteArray {
    val digest = DigestFactory.createSHA3_256()
    digest.update(this, 0, this.size)
    val output = ByteArray(32)
    digest.doFinal(output, 0)
    return output
}