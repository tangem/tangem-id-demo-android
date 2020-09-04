package com.tangem.id

import android.util.Log
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
import com.tangem.id.card.ChangeFilesTask
import com.tangem.id.card.ReadFilesTask
import com.tangem.id.card.issuer
import com.tangem.id.demo.*
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.utils.JsonLdCborEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class HolderData(
    val cardId: String,
    val credentials: List<HolderDemoCredential>
)

class TangemIdHolder(
    private val tangemSdk: TangemSdk,
    private val coroutineScope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private val tapHolderCardMessage =
        Message(activity.getString(R.string.sdk_view_delegate_message_holder))

    private var holderAddress: String? = null

    private var holderCredentials: List<HolderDemoCredential>? = null

    fun showHoldersCredential(index: Int): String {
        return holderCredentials!![index].verifiableCredential.toPrettyJson()
    }

    fun changePasscode(cardId: String?, callback: (SimpleResponse) -> Unit) {
        tangemSdk.changePin2(cardId = cardId, initialMessage = tapHolderCardMessage) { result ->
            when (result) {
                is CompletionResult.Failure -> if (result.error !is TangemSdkError.UserCancelled) {
                    callback(SimpleResponse.Failure(TangemIdError.ReadingCardError(activity)))
                }
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
                    if (result.error !is TangemSdkError.UserCancelled) {
                        callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                    }
                }
                is CompletionResult.Success -> {
                    if (result.data.files.isEmpty() || result.data.files[0].fileData.isEmpty()) {
                        callback(CompletionResult.Failure(TangemIdError.NoCredentials(activity)))
                        return@startSessionWithRunnable
                    }

                    val holderCredentials = result.data.files.mapNotNull { it.toHolderCredential() }
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

    fun changeHoldersCredentials(
        cardId: String?, filesToDelete: List<File>, filesToChangeVisibility: List<File>,
        callback: (SimpleResponse) -> Unit
    ) {
        Log.i(this::class.java.simpleName, "Files to delete: ${filesToDelete.map { "Index ${it.fileIndex}"}}")
        Log.i(this::class.java.simpleName,
            "Files to change visibility: " +
                    "${filesToChangeVisibility.map { "Index ${it.fileIndex}, status: ${it.fileSettings?.name}"}}")

        val task = ChangeFilesTask(filesToDelete, filesToChangeVisibility)
        tangemSdk.startSessionWithRunnable(
            task, initialMessage = tapHolderCardMessage, cardId = cardId
        ) { result ->
            when (result) {
                is CompletionResult.Failure ->
                    if (result.error !is TangemSdkError.UserCancelled) {
                        callback(SimpleResponse.Failure(TangemIdError.ReadingCardError(activity)))
                    }
                is CompletionResult.Success -> {
                    holderCredentials = holderCredentials?.mapNotNull { credential ->
                        if (filesToDelete.find { it.fileIndex == credential.file.fileIndex } != null) {
                            null
                        } else {
                            credential
                        }
                    }
                    holderCredentials = holderCredentials?.map { credential ->
                        if (filesToChangeVisibility.find { it.fileIndex == credential.file.fileIndex } != null) {
                            credential.toggleVisibility()
                        } else {
                            credential
                        }
                    }
                    Log.i(this::class.java.simpleName, "ChangedHolderCredentials: ${holderCredentials?.toString()}")
                    callback(SimpleResponse.Success)
                }
            }
        }
    }

    fun addCovidCredential(
        cardId: String?, callback: (CompletionResult<List<HolderDemoCredential>>) -> Unit
    ) {
        if (holderCredentials?.find { it.demoCredential is DemoCredential.CovidCredential } != null) {
            callback(CompletionResult.Failure(TangemIdError.CredentialAlreadyIssued(activity)))
            return
        }

        coroutineScope.launch {
            val covidResult = DemoCovidCredential.createCovidCredential(
                holderAddress!!, activity.applicationContext
            )
            when (covidResult) {
                is Result.Success -> writeNewCredential(covidResult.data, cardId, callback)
                is Result.Failure ->
                    callback(
                        CompletionResult.Failure(TangemIdError.ErrorAddingNewCredential(activity))
                    )
            }
        }
    }

    private fun writeNewCredential(
        credential: VerifiableCredential, cardId: String?,
        callback: (CompletionResult<List<HolderDemoCredential>>) -> Unit
    ) {
        val encoded = JsonLdCborEncoder.encode(credential.toMap())

        val writeFileDataTask = WriteFileDataTask(encoded, issuer().dataKeyPair)
        tangemSdk.startSessionWithRunnable(
            writeFileDataTask,
            initialMessage = tapHolderCardMessage,
            cardId = cardId
        ) { result ->
            when (result) {
                is CompletionResult.Failure ->
                    if (result.error !is TangemSdkError.UserCancelled) {
                        callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                    }
                is CompletionResult.Success -> {
                    val demoCredential = credential.toDemoCredential()
                    if (demoCredential != null) {
                        val holderCredential = HolderDemoCredential(
                            demoCredential, credential,
                            File(
                                result.data.fileIndex ?: holderCredentials!!.size,
                                FileSettings.Public, encoded
                            )
                        )
                        holderCredentials = holderCredentials!! + holderCredential
                        Log.i(this::class.java.simpleName, holderCredentials?.toString())
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