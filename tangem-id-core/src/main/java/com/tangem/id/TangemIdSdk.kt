package com.tangem.id

import androidx.activity.ComponentActivity
import com.example.tangem_id_core.R
import com.tangem.*
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.Card
import com.tangem.commands.Product
import com.tangem.commands.file.FileSettings
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.CardType
import com.tangem.id.card.ChangeFilesTask
import com.tangem.id.card.ReadFilesTask
import com.tangem.id.card.issuer
import com.tangem.id.card.toggleVisibility
import com.tangem.id.demo.DemoCovidCredential
import com.tangem.id.demo.DemoCredential
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
        savePin1InStaticField = false
    )
    private val tangemSdk = TangemSdk.init(activity, config)

    private var issuerWallet: EthereumIssuerWalletManager? = null
    private var credentialsManager: DemoCredentialsManager? = null
    private var holderAddress: String? = null
    private var holderCardId: String? = null

    private var holderCredentials: List<Pair<VerifiableDemoCredential, FileSettings>>? = null
    private var verifierCredentials: List<VerifiableDemoCredential>? = null

    private var credentials: List<VerifiableCredential>? = null

    private val tapHolderCardMessage =
        Message(activity.getString(R.string.sdk_view_delegate_message_holder))
    private val tapIssuerCardMessage =
        Message(activity.getString(R.string.sdk_view_delegate_message_issuer))

    private fun handleError(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val exceptionAsString: String = sw.toString()
        Log.e("TangemIdSdk", exceptionAsString)
    }

    fun readIssuerCard(callback: (CompletionResult<String>) -> Unit) {
        tangemSdk.scanCard(
            initialMessage = tapIssuerCardMessage
        ) { result ->
            when (result) {
                is CompletionResult.Failure ->
                    callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                is CompletionResult.Success -> {
                    if (result.data.cardData?.productMask?.contains(Product.IdIssuer) != true
                        || !isValidCard(result.data)
                    ) {
                        callback(CompletionResult.Failure(TangemIdError.WrongIssuerCardType(activity)))
                        return@scanCard
                    }

                    issuerWallet = EthereumIssuerWalletManager(result.data)
                    credentialsManager = DemoCredentialsManager(issuerWallet!!, activity, tangemSdk)
                    callback(CompletionResult.Success(issuerWallet!!.wallet.address))
                }
            }

        }

    }

    private fun isValidCard(card: Card): Boolean {
        return card.walletPublicKey != null
                && card.cardData?.blockchainName == Blockchain.Ethereum.id
    }


    fun getHolderAddress(callback: (CompletionResult<String>) -> Unit) {
        tangemSdk.scanCard(
            initialMessage = tapHolderCardMessage
        ) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(
                    CompletionResult.Failure(
                        TangemIdError.ReadingCardError(activity)
                    )
                )
                is CompletionResult.Success -> {
                    if (result.data.cardData?.productMask?.contains(Product.IdCard) != true
                        || result.data.walletPublicKey == null
                    ) {
                        callback(CompletionResult.Failure(TangemIdError.WrongHolderCardType(activity)))
                        return@scanCard
                    }
                    holderCardId = result.data.cardId
                    holderAddress =
                        EthereumAddressService().makeAddress(result.data.walletPublicKey!!)
                    if (holderAddress != null) {
                        callback(CompletionResult.Success(holderAddress!!))
                    } else {
                        callback(CompletionResult.Failure(TangemIdError.CardError(activity)))
                    }
                }
            }
        }
    }

    fun readCredentialsAsHolder(callback: (CompletionResult<HolderData>) -> Unit) {
        tangemSdk.startSessionWithRunnable(
            ReadFilesTask(true),
            initialMessage = tapHolderCardMessage
        ) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                }
                is CompletionResult.Success -> {
                    if (result.data.files.isEmpty() || result.data.files[0].fileData.isEmpty()) {
                        callback(CompletionResult.Failure(TangemIdError.NoCredentials(activity)))
                        return@startSessionWithRunnable
                    }

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
                    holderAddress =
                        EthereumAddressService().makeAddress(result.data.walletPublicKey)
                    if (holderAddress == null) {
                        callback(CompletionResult.Failure(TangemIdError.CardError(activity)))
                        return@startSessionWithRunnable
                    }

                    callback(
                        CompletionResult.Success(
                            HolderData(
                                result.data.cardId,
                                holderCredentials
                            )
                        )
                    )
                }
            }
        }
    }

    fun readCredentialsAsVerifier(callback: (CompletionResult<List<VerifiableDemoCredential>>) -> Unit) {
        tangemSdk.readFileDataTask(readPrivateFiles = false) { result ->
            scope.launch {
                when (result) {
                    is CompletionResult.Failure ->
                        callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                    is CompletionResult.Success -> {
                        if (result.data.files.isEmpty() || result.data.files[0].fileData.isEmpty()) {
                            callback(CompletionResult.Failure(TangemIdError.NoCredentials(activity)))
                            return@launch
                        }

                        verifierCredentials = result.data.files
                            .map { it.fileData }
                            .map { JsonLdCborEncoder.decode(it) }
                            .map { VerifiableCredential.fromMap((it as Map<String, String>)) }
                            .mapNotNull {
                                VerifiableDemoCredential.from(
                                    it
//                                    ,
//                                    it.simpleVerify(activity)
                                )
                            }
                        Log.i("Credentials", verifierCredentials.toString())
                        callback(CompletionResult.Success(verifierCredentials!!))
                    }
                }
            }
        }
    }

    fun formCredentialsAndSign(
        personData: DemoPersonData, holdersAddress: String,
        callback: (SimpleResponse) -> Unit
    ) {
        scope.launch {
            val result = credentialsManager!!
                .createDemoCredentials(personData, holdersAddress, tapIssuerCardMessage)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        credentials = result.data
                        callback(SimpleResponse.Success)
                    }
                    is Result.Failure ->
                        callback(
                            SimpleResponse.Failure(TangemIdError.ErrorCreatingCredentials(activity))
                        )
                }
            }
        }
    }

    fun writeCredentialsAndSend(callback: (SimpleResponse) -> Unit) {
        scope.launch {
            val result = credentialsManager!!.completeWithId(
                credentials!!, tangemSdk, tapHolderCardMessage, holderCardId!!
            )
            withContext(Dispatchers.Main) {
                when (result) {
                    is SimpleResult.Success -> callback(SimpleResponse.Success)
                    is SimpleResult.Failure ->
                        callback(
                            SimpleResponse.Failure(TangemIdError.ErrorWritingCredentials(activity))
                        )
                }
            }
        }
    }

    fun changeHoldersCredentials(
        indicesToDelete: List<Int>, indicesToChangeVisibility: List<Int>,
        callback: (SimpleResponse) -> Unit
    ) {
        val visibilities = holderCredentials!!
            .map { it.second }
            .filterIndexed { index, _ -> indicesToChangeVisibility.contains(index) }
        val task = ChangeFilesTask(indicesToDelete, indicesToChangeVisibility, visibilities)

        tangemSdk.startSessionWithRunnable(
            task, initialMessage = tapHolderCardMessage
        ) { result ->
            when (result) {
                is CompletionResult.Failure ->
                    callback(SimpleResponse.Failure(TangemIdError.ReadingCardError(activity)))

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
                    callback(SimpleResponse.Success)
                }
            }
        }
    }


    fun addCovidCredential(
        callback: (CompletionResult<List<Pair<VerifiableDemoCredential, FileSettings>>>) -> Unit
    ) {
        if (holderCredentials?.find {
                it.first.decodedCredential is DemoCredential.CovidCredential
            } != null) {
            callback(CompletionResult.Failure(TangemIdError.CredentialAlreadyIssued(activity)))
            return
        }

        scope.launch {
            val covidResult =
                DemoCovidCredential.createCovidCredential(
                    holderAddress!!,
                    activity.applicationContext
                )
            when (covidResult) {
                is Result.Failure ->
                    callback(
                        CompletionResult.Failure(
                            TangemIdError.ErrorAddingNewCredential(
                                activity
                            )
                        )
                    )
                is Result.Success -> {
                    val encoded = JsonLdCborEncoder.encode(covidResult.data.toMap())
                    tangemSdk.writeFileDataTask(
                        data = encoded, issuerKeys = issuer().dataKeyPair,
                        initialMessage = tapHolderCardMessage
                    ) { result ->
                        when (result) {
                            is CompletionResult.Failure ->
                                callback(
                                    CompletionResult.Failure(TangemIdError.ReadingCardError(activity))
                                )
                            is CompletionResult.Success -> {
                                val credential = VerifiableDemoCredential.from(covidResult.data)
                                if (credential != null) {
                                    holderCredentials =
                                        holderCredentials!! + (credential to FileSettings.Public)
                                    callback(CompletionResult.Success(holderCredentials!!))
                                } else {
                                    callback(
                                        CompletionResult.Failure(
                                            TangemIdError.ConvertingCredentialError(activity)
                                        )
                                    )
                                }
                            }


                        }

                    }
                }
            }
        }
    }

    fun changePasscode(callback: (SimpleResponse) -> Unit) {
        tangemSdk.changePin2(initialMessage = tapHolderCardMessage) { result ->
            when (result) {
                is CompletionResult.Failure ->
                    callback(SimpleResponse.Failure(TangemIdError.ReadingCardError(activity)))
                is CompletionResult.Success -> callback(SimpleResponse.Success)
            }
        }
    }

    fun showJsonWhileCreating(): String {
        return credentialsManager!!.showCredentials().joinToString("\n")
    }

    fun showHoldersCredential(index: Int): String {
        return holderCredentials!![index].first.verifiableCredential.toPrettyJson()
    }

    fun showVerifierCredentials(): String {
        return verifierCredentials!!.map { it.verifiableCredential.toPrettyJson() }
            .joinToString("\n")
    }

}


data class HolderData(
    val cardId: String,
    val credentials: List<Pair<VerifiableDemoCredential, FileSettings>>
)

sealed class SimpleResponse {
    object Success : SimpleResponse()
    data class Failure(val error: TangemError) : SimpleResponse()
}