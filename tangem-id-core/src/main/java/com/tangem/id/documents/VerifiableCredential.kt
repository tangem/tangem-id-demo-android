package com.tangem.id.documents

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.tangem.id.proof.Secp256k1Proof
import java.time.Instant

@JsonClass(generateAdapter = true)
open class VerifiableCredential(
    val credentialSubject: Map<String, Any>,
    val issuer: String,
    val issuanceDate: String,
    val validFrom: String?,
    val id: String?,
    var ethCredentialStatus: String?,
    @Json(name = "@context") context: Collection<String>,
    type: Collection<String>,
    proof: Secp256k1Proof?
) : VerifiableDocument(context.toMutableSet(), type.toMutableSet(), proof) {

    constructor(
        credentialSubject: Map<String, Any>,
        issuer: String,
        extraContexts: Collection<String>? = null,
        extraTypes: Collection<String>? = null,
        validFrom: String? = null,
        id: String? = null
    ) : this(
        credentialSubject = credentialSubject,
        issuer = issuer,
        issuanceDate = Instant.now().toString(),
        validFrom = validFrom,
        id = id,
        ethCredentialStatus = null,
        context = setOf(DEFAULT_CONTEXT),
        type = setOf(DEFAULT_TYPE),
        proof = null
    ) {
        if (extraContexts != null) context.addAll(extraContexts)
        if (extraTypes != null) type.addAll(extraTypes)
    }

    override fun toJson(): String = jsonAdapter.toJson(this)
    fun toPrettyJson(): String = jsonAdapter.indent("  ").toJson(this)

    @Suppress("UNCHECKED_CAST")
    fun toMap(): Map<String, Any> {
        return jsonAdapter.toJsonValue(this) as Map<String, Any>
    }

    companion object {
        const val DEFAULT_TYPE = "VerifiableCredential"
        const val TANGEM_ETH_CREDENTIAL = "TangemEthCredential"
//        const val TANGEM_DEMO_CONTEXT =
//            "https://tangem.com/context/demo" // TODO: set actual context

        private val jsonAdapter =
            Moshi.Builder().build().adapter(VerifiableCredential::class.java)

        fun fromJson(jsonString: String): VerifiableCredential { // TODO: change return type to Result
            return jsonAdapter.fromJson(jsonString)!!
        }

        fun fromMap(map: Map<String, String>): VerifiableCredential { // TODO: change return type to Result
            return jsonAdapter.fromJsonValue(map)!!
        }
    }
}