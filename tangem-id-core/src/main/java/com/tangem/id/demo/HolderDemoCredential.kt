package com.tangem.id.demo

import com.tangem.id.card.toggleVisibility
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.utils.JsonLdCborEncoder
import com.tangem.tasks.file.File

data class HolderDemoCredential(
    val demoCredential: DemoCredential,
    val verifiableCredential: VerifiableCredential,
    val file: File
) {
    fun toggleVisibility(): HolderDemoCredential {
        return this.copy(file = file.copy(fileSettings = file.fileSettings?.toggleVisibility()))
    }

    override fun toString(): String {
        return "Credential: ${demoCredential::class.java.simpleName}," +
                " fileIndex: ${file.fileIndex}, fileStatus: ${file.fileSettings?.name}"
    }
}

fun File.toHolderCredential(): HolderDemoCredential? {
    val verifiableCredential = VerifiableCredential
        .fromMap((JsonLdCborEncoder.decode(this.fileData) as Map<String, String>))
    val demoCredential = verifiableCredential.toDemoCredential()
    return if (demoCredential != null) {
        HolderDemoCredential(demoCredential, verifiableCredential, this)
    } else {
        null
    }
}