package com.tangem.id.demo

import com.microsoft.did.sdk.util.formVerifiableCredential
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.documents.VerifiableDocument
import com.tangem.id.utils.JsonLdCborEncoder
import com.tangem.tasks.file.File
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.microsoft.did.sdk.credential.models.VerifiableCredential as MSVerifiableCredential

abstract class VerifierDemoCredential(
) {
    abstract val decodedCredential: DemoCredential
    open val verified: Boolean? = null

    companion object {

        fun from( //TODO: remove?
            verifiableCredential: VerifiableCredential,
            verified: Boolean? = null
        ): VerifierDemoCredential? {
            val demoCredential: DemoCredential? = verifiableCredential.toDemoCredential()
            return if (demoCredential == null) {
                null
            } else {
                TangemVerifierDemoCredential(
                    verifiableCredential = verifiableCredential,
                    decodedCredential = demoCredential,
                    verified = verified
                )
            }
        }
    }
}

data class TangemVerifierDemoCredential(
    override val decodedCredential: DemoCredential,
    override val verified: Boolean? = null,
    val verifiableCredential: VerifiableCredential
) : VerifierDemoCredential()

data class MSVerifierDemoCredential(
    override val decodedCredential: DemoCredential,
    override val verified: Boolean? = null,
    val verifiableCredential: MSVerifiableCredential
) : VerifierDemoCredential()

fun File.toVerifierCredential(): VerifierDemoCredential? {
    return try {
        val verifiableCredential = VerifiableCredential
            .fromMap((JsonLdCborEncoder.decode(this.fileData) as Map<String, String>))
        val demoCredential = verifiableCredential.toDemoCredential() ?: return null
        TangemVerifierDemoCredential(demoCredential, true, verifiableCredential) // TODO: change "true" to "verifiableCredential.simpleVerify()"

    } catch (exception: Exception) {
        val token = String(this.fileData)
        val msVerifiableCredential = formVerifiableCredential(token, Json)
        val demoCredential = msVerifiableCredential.toDemoCredential() ?: return null
        MSVerifierDemoCredential(demoCredential, true, msVerifiableCredential)
    }
}

suspend fun VerifiableDocument.simpleVerify() = this.verify() is SimpleResult.Success

fun String.toDate(): LocalDate? =
    if (this.contains("/")) {
        try {
            LocalDate.parse(this, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        } catch (exception: Exception) {
            null
        }
    } else {
        try {
            val builder = StringBuilder(this)
            builder.insert(2, "/")
            builder.insert(5, "/")
            LocalDate.parse(builder.toString(), DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        } catch (exception: Exception) {
            null
        }
    }
