package com.tangem.id.proof

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.json.JSONObject
import java.time.Instant

@JsonClass(generateAdapter = true)
open class LinkedDataProof internal constructor(
    val type: String,
    val verificationMethod: String,
    val challenge: String?,
    val proofPurpose: String,
    val created: String,
    var jws: String?
) {

    constructor(
        type: String,
        verificationMethod: String,
        challenge: String? = null
    ) : this(
        type = type,
        verificationMethod = verificationMethod,
        challenge = challenge,
        proofPurpose = ASSERTION_METHOD,
        created = Instant.now().toString(),
        jws = null
    )

    fun toJson(): String = jsonAdapter.toJson(this)

    @Suppress("UNCHECKED_CAST")
    fun toMap(): Map<String, String> {
        return jsonAdapter.toJsonValue(this) as Map<String, String>
    }

    fun toJSONObject() = JSONObject(this.toJson())

    companion object {
        const val ASSERTION_METHOD = "assertionMethod"

        private val jsonAdapter =
            Moshi.Builder().build().adapter(LinkedDataProof::class.java)

        fun fromJson(jsonString: String): LinkedDataProof { // TODO: change return type to Result
            return jsonAdapter.fromJson(jsonString)!!
        }

        fun fromJson(jsonObject: JSONObject): LinkedDataProof { // TODO: change return type to Result
            return jsonAdapter.fromJson(jsonObject.toString())!!
        }

        fun fromMap(map: Map<String, String>): LinkedDataProof { // TODO: change return type to Result
            return jsonAdapter.fromJsonValue(map)!!
        }
    }
}

