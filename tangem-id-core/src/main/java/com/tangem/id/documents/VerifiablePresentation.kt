package com.tangem.id.documents

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.tangem.id.proof.LinkedDataProof
import com.tangem.id.proof.Secp256k1Proof

@JsonClass(generateAdapter = true)
class VerifiablePresentation internal constructor(
    @Json(name = "verifiableCredential") val credentials: Collection<VerifiableCredential>,
    @Json(name = "@context") context: Collection<String>,
    type: Collection<String>,
    proof: LinkedDataProof?
) : VerifiableDocument(context.toMutableSet(), type.toMutableSet(), proof) {

    constructor(
        credentials: Collection<VerifiableCredential>,
        extraContexts: Collection<String>? = null,
        extraTypes: Collection<String>? = null
    ) : this(
        credentials = credentials,
        context = setOf(DEFAULT_CONTEXT),
        type = setOf(DEFAULT_TYPE),
        proof = null
    ) {
        if (extraContexts != null) context.addAll(extraContexts)
        if (extraTypes != null) type.addAll(extraTypes)
    }

    override fun toJson(): String = jsonAdapter.toJson(this)

    @Suppress("UNCHECKED_CAST")
    fun toMap(): Map<String, Any> {
        return jsonAdapter.toJsonValue(this) as Map<String, Any>
    }

    companion object {
        const val DEFAULT_TYPE = "VerifiablePresentation"

        private val jsonAdapter =
            Moshi.Builder().build().adapter(VerifiablePresentation::class.java)

        fun fromJson(jsonString: String): VerifiablePresentation { // TODO: change return type to Result
            return jsonAdapter.fromJson(jsonString)!!
        }

        fun fromMap(map: Map<String, Any>): VerifiablePresentation { // TODO: change return type to Result
            return jsonAdapter.fromJsonValue(map)!!
        }
    }
}