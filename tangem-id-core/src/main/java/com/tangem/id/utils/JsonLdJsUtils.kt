package com.tangem.id.utils

import android.content.Context
import com.tangem.blockchain.extensions.Result
import org.json.JSONObject
import org.liquidplayer.service.MicroService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


suspend fun normalizeJsonLd(jsonObject: JSONObject, androidContext: Context): Result<String> {
    return try {
        suspendCoroutine { continuation ->
            val readyListener = MicroService.EventListener { service, _, _ ->
                service.emit("normalize", jsonObject)
            }
            val normalizeListener = MicroService.EventListener { _, _, normalizeResponse ->
                val normalized = normalizeResponse.getString("_")
                continuation.resume(Result.Success(normalized))
            }
            val startListener =
                MicroService.ServiceStartListener { service ->
                    service.addEventListener("ready", readyListener)
                    service.addEventListener("normalized", normalizeListener)
                }
            val errorListener =
                MicroService.ServiceErrorListener { _, exception ->
                    continuation.resumeWithException(exception)
                }

            val uri = MicroService.Bundle(androidContext, "index")
            val service = MicroService(androidContext, uri, startListener, errorListener)
            service.start()
        }
    } catch (exception: Exception) {
        Result.Failure(exception)
    }
}