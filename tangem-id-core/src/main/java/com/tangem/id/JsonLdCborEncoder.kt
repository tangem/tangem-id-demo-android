package com.tangem.id

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory

class JsonLdCborEncoder {

    fun encode(jsonLdObject: Map<String, Any>): ByteArray { // TODO: comply with https://w3c.github.io/json-ld-cbor/
        val cborFactory = CBORFactory()
        val mapper = ObjectMapper(cborFactory)
        return mapper.writeValueAsBytes(jsonLdObject)
    }

    fun decode(cborData: ByteArray): Map<String, Any> { // TODO: comply with https://w3c.github.io/json-ld-cbor/
        val cborFactory = CBORFactory()
        val mapper = ObjectMapper(cborFactory)
        return mapper.readValue(cborData, Map::class.java) as Map<String, Any>
    }
}