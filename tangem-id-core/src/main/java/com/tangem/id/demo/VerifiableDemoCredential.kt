package com.tangem.id.demo

import android.content.Context
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.documents.VerifiableDocument
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VerifiableDemoCredential(
    val verifiableCredential: VerifiableCredential,
    val decodedCredential: DemoCredential,
    val verified: Boolean? = null
) {
    companion object {

        fun from(
            verifiableCredential: VerifiableCredential,
            verified: Boolean? = null
        ): VerifiableDemoCredential? {
            val demoCredential: DemoCredential? = verifiableCredential.toDemoCredential()
            return if (demoCredential == null) {
                null
            } else {
                VerifiableDemoCredential(
                    verifiableCredential = verifiableCredential, decodedCredential = demoCredential,
                    verified = verified
                )
            }
        }
    }
}

suspend fun VerifiableDocument.simpleVerify(androidContext: Context): Boolean {
    val result = this.verifyProof(androidContext)
    return result is SimpleResult.Success
}

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
