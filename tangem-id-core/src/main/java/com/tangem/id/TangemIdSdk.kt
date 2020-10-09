package com.tangem.id

import androidx.activity.ComponentActivity
import com.tangem.*
import com.tangem.common.extensions.CardType
import com.tangem.tangem_sdk_new.extensions.init
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.coroutines.CoroutineContext

class TangemIdSdk(val activity: ComponentActivity) {

    private val parentJob = Job()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO + exceptionHandler
    private val scope = CoroutineScope(coroutineContext)

    private val config = Config(
        cardFilter = CardFilter(EnumSet.allOf(CardType::class.java))
    )
    val tangemSdk = TangemSdk.init(activity, config)

    val issuer = TangemIdIssuer(tangemSdk, scope, activity)
    val holder = TangemIdHolder(tangemSdk, scope, activity)
    val verifier = TangemIdVerifier(tangemSdk, scope, activity)

    private fun handleError(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val exceptionAsString: String = sw.toString()
        Log.e("TangemIdSdk", exceptionAsString)
        throw throwable
    }
}

sealed class SimpleResponse {
    object Success : SimpleResponse()
    data class Failure(val error: TangemError) : SimpleResponse()
}


