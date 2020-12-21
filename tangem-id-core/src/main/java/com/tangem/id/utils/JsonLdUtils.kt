package com.tangem.id.utils

import com.tangem.blockchain.extensions.Result
import com.tangem.jsonld.JsonLd
import com.tangem.jsonld.document.JsonDocument
import com.tangem.rdf.normalization.NQuadSerializer
import com.tangem.rdf.normalization.RdfNormalize


suspend fun normalizeJsonLd(jsonString: String): Result<String> {
    return try {
        val jsonDocument = JsonDocument.of(jsonString.byteInputStream())
        val rdfDataset = JsonLd.toRdf(jsonDocument).get()
        val normalizedDataset = RdfNormalize.normalize(rdfDataset)

        var normalizedString = ""
        normalizedDataset.toList().forEach { normalizedString += NQuadSerializer.write(it) }

        Result.Success(normalizedString)
    } catch (exception: Exception) {
        Result.Failure(exception)
    }
}