package com.tangem.id

import android.util.Log
import androidx.activity.ComponentActivity
import com.example.tangem_id_core.R
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.TangemSdkError
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.extensions.Result
import com.tangem.commands.file.FileData
import com.tangem.commands.file.FileSettings
import com.tangem.common.CompletionResult
import com.tangem.id.card.ChangeFilesTask
import com.tangem.id.card.ReadFilesAndDataTask
import com.tangem.id.demo.*
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.utils.JsonLdCborEncoder
import com.tangem.tasks.file.File
import com.tangem.tasks.file.WriteFilesTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.microsoft.did.sdk.credential.models.VerifiableCredential as MSVerifiableCredential

data class HolderData(
    val cardId: String,
    val walletPublicKey: ByteArray,
    val credentials: List<HolderDemoCredential>
)

class TangemIdHolder(
    val tangemSdk: TangemSdk,
    val coroutineScope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private val tapHolderCardMessage =
        Message(activity.getString(R.string.sdk_view_delegate_message_holder))

    private var holderAddress: String? = null

    private var holderCredentials: List<HolderDemoCredential>? = null

    fun showRawHoldersCredential(index: Int): String {
        return when (val credential = holderCredentials!![index]) {
            is TangemHolderDemoCredential -> credential.verifiableCredential.toPrettyJson()
            is MSHolderDemoCredential -> credential.verifiableCredential.raw
            else -> "Unknown credential type" //TODO: throw?
        }
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
            ReadFilesAndDataTask(true),
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
                        CompletionResult.Success(HolderData(result.data.cardId, result.data.walletPublicKey, holderCredentials))
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

    fun addCovidCredential( // TODO: remove?
        cardId: String?, callback: (CompletionResult<List<HolderDemoCredential>>) -> Unit
    ) {
        if (holderCredentials?.find { it.demoCredential is DemoCredential.CovidCredential } != null) {
            callback(CompletionResult.Failure(TangemIdError.CredentialAlreadyIssued(activity)))
            return
        }

        coroutineScope.launch {
            when (val covidResult = DemoCovidCredential.createCovidCredential(holderAddress!!)) {
                is Result.Success -> writeNewCredential(covidResult.data, cardId, callback)
                is Result.Failure ->
                    callback(
                        CompletionResult.Failure(TangemIdError.ErrorAddingNewCredential(activity))
                    )
            }
        }
    }

    fun addVCExpertCredential(
        credential: MSVerifiableCredential,
        cardId: String?,
        callback: (CompletionResult<List<HolderDemoCredential>>) -> Unit
    ) {
        if (holderCredentials?.find { it.demoCredential is DemoCredential.VCExpertCredential } != null) {
            callback(CompletionResult.Failure(TangemIdError.CredentialAlreadyIssued(activity)))
            return
        }
//        coroutineScope.launch { writeNewMSCredential(credential, cardId, callback) } //TODO: now already called in coroutineScope
        writeNewMSCredential(credential, cardId, callback)
    }

    private fun writeNewCredential(
        credential: VerifiableCredential, cardId: String?,
        callback: (CompletionResult<List<HolderDemoCredential>>) -> Unit
    ) {
        val encoded = JsonLdCborEncoder.encode(credential.toMap())
        val writeFilesTask = WriteFilesTask(listOf(FileData.DataProtectedByPasscode(encoded)))

        tangemSdk.startSessionWithRunnable(
            writeFilesTask,
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
                        val holderCredential = TangemHolderDemoCredential(
                            File(
                                result.data.fileIndex ?: holderCredentials!!.size,
                                FileSettings.Public, encoded
                            ),
                            demoCredential,
                            credential
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

    private fun writeNewMSCredential(
        credential: MSVerifiableCredential, cardId: String?,
        callback: (CompletionResult<List<HolderDemoCredential>>) -> Unit
    ) {
        val tokenBytes = credential.raw.toByteArray()
        val writeFilesTask = WriteFilesTask(listOf(FileData.DataProtectedByPasscode(tokenBytes)))

        tangemSdk.startSessionWithRunnable(
            writeFilesTask,
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
                        val holderCredential = MSHolderDemoCredential(
                            File(
                                result.data.fileIndex ?: holderCredentials!!.size,
                                FileSettings.Public,
                                tokenBytes
                            ),
                            demoCredential,
                            credential
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