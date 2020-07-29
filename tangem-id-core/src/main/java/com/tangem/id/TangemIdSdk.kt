package com.tangem.id

import androidx.activity.ComponentActivity
import com.tangem.CardFilter
import com.tangem.Config
import com.tangem.TangemSdk
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Signer
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.Card
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.CardType
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.utils.JsonLdCborEncoder
import com.tangem.tangem_sdk_new.extensions.init
import kotlinx.coroutines.*
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

    private var issuerCard: Card? = null
    private var issuerWallet: EthereumIssuerWalletManager? = null
    private var credentialsManager: DemoCredentialsManager? = null
    private var holderAddress: String? = null

    private var credentials: List<VerifiableCredential>? = null

    private fun handleError(throwable: Throwable) {
        throw throwable
    }

    fun readIssuerCard(callback: (String?) -> Unit) {
        tangemSdk.scanCard { result ->
            when (result) {
                is CompletionResult.Failure -> callback(null)
                is CompletionResult.Success -> {
                    issuerWallet = EthereumIssuerWalletManager(result.data)
                    credentialsManager = DemoCredentialsManager(
                        issuerWallet!!,
                        activity.applicationContext
                    )
                    callback(issuerWallet!!.wallet.address)
                }
            }
        }
    }


    fun getHolderAddress(callback: (String?) -> Unit) {
        tangemSdk.scanCard { result ->
            when (result) {
                is CompletionResult.Failure -> callback(null)
                is CompletionResult.Success -> {
                    holderAddress =
                        EthereumAddressService().makeAddress(result.data.walletPublicKey!!)
                    callback(holderAddress)
                }
            }
        }
    }

    fun readCredentialsAsHolder(callback: (List<Map<String, Any>>?) -> Unit) {
        tangemSdk.readFileDataTask(readPrivateFiles = true) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(null)
                is CompletionResult.Success -> {
                    val credentialsInBytes = result.data.files
                        .map { it.fileData }
                        .map { JsonLdCborEncoder.decode(it) }
                    callback(credentialsInBytes)
                }
            }
        }
    }

    fun readCredentialsAsVerifier(callback: (List<Map<String, Any>>?) -> Unit) {
        tangemSdk.readFileDataTask(readPrivateFiles = false) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(null)
                is CompletionResult.Success -> {
                    val credentialsInBytes = result.data.files
                        .map { it.fileData }
                        .map { JsonLdCborEncoder.decode(it) }
                    callback(credentialsInBytes)
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
            when (result) {
                is Result.Success -> {
                    credentials = result.data
                    callback(SimpleResult.Success)
                }
                is Result.Failure -> callback(SimpleResult.Failure(Error()))
            }
        }
    }

    fun writeCredentialsAndSend(callback: (SimpleResult) -> Unit) {
        scope.launch {
            callback(credentialsManager!!.completeWithId(credentials!!, tangemSdk))
        }
    }

    fun addCredential() {

    }

}