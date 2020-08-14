package com.tangem.id

import androidx.activity.ComponentActivity
import com.example.tangem_id_core.R
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.TangemSdkError
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.extensions.Result
import com.tangem.commands.file.File
import com.tangem.commands.file.FileSettings
import com.tangem.commands.file.WriteFileDataTask
import com.tangem.common.CompletionResult
import com.tangem.id.card.*
import com.tangem.id.demo.DemoCovidCredential
import com.tangem.id.demo.DemoCredential
import com.tangem.id.demo.VerifiableDemoCredential
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.utils.JsonLdCborEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class HolderData(
    val cardId: String,
    val credentials: List<Pair<VerifiableDemoCredential, FileSettings>>
)

class TangemIdHolder(
    private val tangemSdk: TangemSdk,
    private val coroutineScope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private val tapHolderCardMessage =
        Message(activity.getString(R.string.sdk_view_delegate_message_holder))

    private var holderAddress: String? = null

    private var holderCredentials: List<Pair<VerifiableDemoCredential, FileSettings>>? = null

    fun showHoldersCredential(index: Int): String {
        return holderCredentials!![index].first.verifiableCredential.toPrettyJson()
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

    fun readCredentialsAsHolder(callback: (CompletionResult<HolderData>) -> Unit) {
        tangemSdk.startSessionWithRunnable(
            ReadFilesTask(true),
            initialMessage = tapHolderCardMessage
        ) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.CardError) {
                        callback(CompletionResult.Failure(TangemIdError.WrongHolderCardType(activity)))
                        return@startSessionWithRunnable
                    }
                    callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                }
                is CompletionResult.Success -> {
                    if (result.data.files.isEmpty() || result.data.files[0].fileData.isEmpty()) {
                        callback(CompletionResult.Failure(TangemIdError.NoCredentials(activity)))
                        return@startSessionWithRunnable
                    }

                    val holderCredentials = result.data.files.toHolderDemoCredentials()
                    this.holderCredentials = holderCredentials

                    holderAddress =
                        EthereumAddressService().makeAddress(result.data.walletPublicKey)

                    callback(
                        CompletionResult.Success(HolderData(result.data.cardId, holderCredentials))
                    )
                }
            }
        }
    }

    private fun List<File>.toHolderDemoCredentials(): List<Pair<VerifiableDemoCredential, FileSettings>> {
        val visibility =
            this.map { it.fileSettings ?: FileSettings.Public }

        return this.toVerifiableCredentials()
            .zip(visibility)
            .mapNotNull {
                VerifiableDemoCredential.from(it.first)
                    ?.let { demoCredential -> demoCredential to it.second }
            }
    }

    fun changeHoldersCredentials(
        indicesToDelete: List<Int>, indicesToChangeVisibility: List<Int>,
        callback: (SimpleResponse) -> Unit
    ) {
        val visibilitiesToChange = holderCredentials!!
            .map { it.second }
            .filterIndexed { index, _ -> indicesToChangeVisibility.contains(index) }
        val task = ChangeFilesTask(indicesToDelete, indicesToChangeVisibility, visibilitiesToChange)

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

        coroutineScope.launch {
            val covidResult = DemoCovidCredential.createCovidCredential(
                holderAddress!!, activity.applicationContext
            )
            when (covidResult) {
                is Result.Success -> writeNewCredential(covidResult.data, callback)
                is Result.Failure ->
                    callback(
                        CompletionResult.Failure(TangemIdError.ErrorAddingNewCredential(activity))
                    )
            }
        }
    }

    private fun writeNewCredential(
        credential: VerifiableCredential,
        callback: (CompletionResult<List<Pair<VerifiableDemoCredential, FileSettings>>>) -> Unit
    ) {
        val encoded = JsonLdCborEncoder.encode(credential.toMap())

        val writeFIleDataTask = WriteFileDataTask(encoded, issuer().dataKeyPair)
        tangemSdk.startSessionWithRunnable(
            writeFIleDataTask,
            initialMessage = tapHolderCardMessage
        ) { result ->
            when (result) {
                is CompletionResult.Failure ->
                    callback(
                        CompletionResult.Failure(TangemIdError.ReadingCardError(activity))
                    )
                is CompletionResult.Success -> {
                    val demoCredential = VerifiableDemoCredential.from(credential)
                    if (demoCredential != null) {
                        holderCredentials =
                            holderCredentials!! + (demoCredential to FileSettings.Public)
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