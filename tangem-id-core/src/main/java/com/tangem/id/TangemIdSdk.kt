package com.tangem.id

import androidx.activity.ComponentActivity
import com.tangem.CardFilter
import com.tangem.Config
import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Signer
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.file.FileSettings
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.CardType
import com.tangem.id.card.ChangeFilesTask
import com.tangem.id.card.ReadFilesTask
import com.tangem.id.card.issuer
import com.tangem.id.card.toggleVisibility
import com.tangem.id.demo.DemoCovidCredential
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.demo.VerifiableDemoCredential
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.utils.JsonLdCborEncoder
import com.tangem.tangem_sdk_new.extensions.init
import kotlinx.coroutines.*
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
        cardFilter = CardFilter(EnumSet.allOf(CardType::class.java)),
        savePin2InStaticField = true
    )
    private val tangemSdk = TangemSdk.init(activity, config)

    private var issuerWallet: EthereumIssuerWalletManager? = null
    private var credentialsManager: DemoCredentialsManager? = null
    private var holderAddress: String? = null

    private var holderCredentials: List<Pair<VerifiableDemoCredential, FileSettings>>? = null
    private var verifierCredentials: List<VerifiableDemoCredential>? = null

    private var credentials: List<VerifiableCredential>? = null

    private fun handleError(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val exceptionAsString: String = sw.toString()
        Log.e("TangemIdSdk", exceptionAsString)
    }

    fun readIssuerCard(callback: (String?) -> Unit) {
        tangemSdk.scanCard { result ->
            when (result) {
                is CompletionResult.Failure -> scope.launch(Dispatchers.Main) { callback(null) }
                is CompletionResult.Success -> {
                    issuerWallet = EthereumIssuerWalletManager(result.data)
                    credentialsManager = DemoCredentialsManager(
                        issuerWallet!!,
                        activity.applicationContext
                    )
                    scope.launch(Dispatchers.Main) { callback(issuerWallet!!.wallet.address) }
                }
            }

        }

    }


    fun getHolderAddress(callback: (String?) -> Unit) {
        tangemSdk.scanCard { result ->
            when (result) {
                is CompletionResult.Failure -> scope.launch(Dispatchers.Main) { callback(null) }
                is CompletionResult.Success -> {
                    holderAddress =
                        EthereumAddressService().makeAddress(result.data.walletPublicKey!!)
                    scope.launch(Dispatchers.Main) { callback(holderAddress) }
                }
            }
        }
    }

    fun readCredentialsAsHolder(callback: (Result<HolderData>) -> Unit) {
        tangemSdk.startSessionWithRunnable(ReadFilesTask(true)) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    scope.launch(Dispatchers.Main) { callback(Result.Failure(Exception())) }
                }
                is CompletionResult.Success -> {
                    val visibility =
                        result.data.files.map { it.fileSettings ?: FileSettings.Public }
                    val holderCredentials = result.data.files
                        .map { it.fileData }
                        .map { JsonLdCborEncoder.decode(it) }
                        .map { VerifiableCredential.fromMap((it as Map<String, String>)) }
                        .zip(visibility)
                        .mapNotNull {
                            VerifiableDemoCredential.from(it.first)
                                ?.let { demoCredential -> demoCredential to it.second }
                        }
                    this.holderCredentials = holderCredentials
                    Log.i("Credentials", holderCredentials.toString())
                    scope.launch(Dispatchers.Main) {
                        callback(Result.Success(HolderData(result.data.cardId, holderCredentials)))
                    }
                }
            }
        }
    }

    fun readCredentialsAsVerifier(callback: (List<VerifiableDemoCredential>?) -> Unit) {
        tangemSdk.readFileDataTask(readPrivateFiles = false) { result ->
            when (result) {
                is CompletionResult.Failure -> scope.launch(Dispatchers.Main) { callback(null) }
                is CompletionResult.Success -> {
                    verifierCredentials = result.data.files
                        .map { it.fileData }
                        .map { JsonLdCborEncoder.decode(it) }
                        .map { VerifiableCredential.fromMap((it as Map<String, String>)) }
                        .mapNotNull { VerifiableDemoCredential.from(it) }
                    Log.i("Credentials", verifierCredentials.toString())
                    scope.launch(Dispatchers.Main) { callback(verifierCredentials) }
                }
            }
        }
    }

    fun formCredentialsAndSign(
        personData: DemoPersonData, holdersAddress: String,
        callback: (SimpleResult) -> Unit
    ) {
        scope.launch {
            val result = credentialsManager!!.createDemoCredentials(
                personData, holdersAddress, Signer(tangemSdk)
            )
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        credentials = result.data
                        callback(SimpleResult.Success)
                    }
                    is Result.Failure -> callback(SimpleResult.Failure(Error()))
                }
            }
        }
    }

    fun writeCredentialsAndSend(callback: (SimpleResult) -> Unit) {
        scope.launch {
            val result = credentialsManager!!.completeWithId(credentials!!, tangemSdk)
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    fun changeHoldersCredentials(
        indicesToDelete: List<Int>, indicesToChangeVisibility: List<Int>,
        callback: (SimpleResult) -> Unit
    ) {
        val task = ChangeFilesTask(
            indicesToDelete, indicesToChangeVisibility,
            holderCredentials!!.unzip().second
        )

        tangemSdk.startSessionWithRunnable(task) { result ->
            when (result) {
                is CompletionResult.Failure -> scope.launch(Dispatchers.Main) {
                    callback(SimpleResult.failure(result.error))
                }
                is CompletionResult.Success -> {
                    holderCredentials = holderCredentials
                        ?.mapIndexed { index, pair ->
                            if (indicesToChangeVisibility.contains(index)) {
                                pair.first to pair.second.toggleVisibility()
                            } else {
                                pair
                            }
                        }
                        ?.mapIndexed { index, pair ->
                            if (indicesToDelete.contains(index)) null else pair
                        }?.filterNotNull()
                    scope.launch(Dispatchers.Main) { callback(SimpleResult.Success) }
                }
            }
        }
    }


    fun addCovidCredential(holderAddress: String, callback: (SimpleResult) -> Unit) {
        scope.launch {
            val covidResult =
                DemoCovidCredential.createCovidCredential(
                    holderAddress,
                    activity.applicationContext
                )
            when (covidResult) {
                is Result.Failure -> withContext(Dispatchers.Main) {
                    callback(SimpleResult.Failure(covidResult.error))
                }
                is Result.Success -> {
                    val encoded = JsonLdCborEncoder.encode(covidResult.data.toMap())
                    tangemSdk.writeFileDataTask(
                        data = encoded, issuerKeys = issuer().dataKeyPair
                    ) { result ->

                        when (result) {
                            is CompletionResult.Failure -> TODO()
                            is CompletionResult.Success -> TODO()
                        }

                    }
                }
            }
        }
    }

}


data class HolderData(
    val cardId: String,
    val credentials: List<Pair<VerifiableDemoCredential, FileSettings>>
)