package com.tangem.id.demo

import com.tangem.id.card.toggleVisibility
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.utils.JsonLdCborEncoder
import com.tangem.id.utils.MSVeriviableCredentialFromString
import com.tangem.tasks.file.File
import com.microsoft.did.sdk.credential.models.VerifiableCredential as MSVerifiableCredential

abstract class HolderDemoCredential {
    abstract val file: File
    abstract val demoCredential: DemoCredential

    abstract fun toggleVisibility(): HolderDemoCredential

    override fun toString(): String {
        return "Credential: ${demoCredential::class.java.simpleName}," +
                " fileIndex: ${file.fileIndex}, fileStatus: ${file.fileSettings?.name}"
    }
}

data class TangemHolderDemoCredential(
    override val file: File,
    override val demoCredential: DemoCredential,
    val verifiableCredential: VerifiableCredential
) : HolderDemoCredential() {
    override fun toggleVisibility(): HolderDemoCredential {
        return this.copy(file = file.copy(fileSettings = file.fileSettings?.toggleVisibility()))
    }
}

data class MSHolderDemoCredential(
    override val file: File,
    override val demoCredential: DemoCredential,
    val verifiableCredential: MSVerifiableCredential
) : HolderDemoCredential() {
    override fun toggleVisibility(): HolderDemoCredential {
        return this.copy(file = file.copy(fileSettings = file.fileSettings?.toggleVisibility()))
    }
}

fun File.toHolderCredential(): HolderDemoCredential? {
    return try {
        val verifiableCredential = VerifiableCredential
            .fromMap((JsonLdCborEncoder.decode(this.fileData) as Map<String, String>))
        val demoCredential = verifiableCredential.toDemoCredential() ?: return null
        TangemHolderDemoCredential(this, demoCredential, verifiableCredential)

    } catch (exception: Exception) {
        val tokenString = String(this.fileData)
        val msVerifiableCredential = MSVeriviableCredentialFromString(tokenString)
        val demoCredential = msVerifiableCredential.toDemoCredential() ?: return null
        MSHolderDemoCredential(this, demoCredential, msVerifiableCredential)
    }

}