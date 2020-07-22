package com.tangem.id.documents

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.tangem.id.proof.LinkedDataProof
import java.time.Instant

@JsonClass(generateAdapter = true)
class VerifiableCredential(
    val credentialSubject: Map<String, Any>,
    val issuer: String,
    val issuanceDate: String,
    val validFrom: String?,
    var id: String?,
    @Json(name = "@context") context: Collection<String>,
    type: Collection<String>,
    proof: LinkedDataProof?
) : VerifiableDocument(context.toMutableSet(), type.toMutableSet(), proof) {

    constructor(
        credentialSubject: Map<String, Any>,
        issuer: String,
        extraContexts: Collection<String>? = null,
        extraTypes: Collection<String>? = null,
        validFrom: String? = null
    ) : this(
        credentialSubject = credentialSubject,
        issuer = issuer,
        issuanceDate = Instant.now().toString(),
        validFrom = validFrom,
        id = null,
        context = setOf(DEFAULT_CONTEXT),
        type = setOf(DEFAULT_TYPE),
        proof = null
    ) {
        if (extraContexts != null) context.addAll(extraContexts)
        if (extraTypes != null) type.addAll(extraTypes)
    }

    override fun toJson(): String = jsonAdapter.toJson(this)

    companion object {
        const val DEFAULT_TYPE = "VerifiableCredential"

        private val jsonAdapter =
            Moshi.Builder().build().adapter(VerifiableCredential::class.java)

        fun fromJson(jsonString: String): VerifiableCredential { // TODO: change return type to Result
            return jsonAdapter.fromJson(jsonString)!!
        }
    }
}