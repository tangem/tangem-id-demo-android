package com.tangem.id.card

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemSdkError
import com.tangem.commands.CommandResponse
import com.tangem.commands.common.card.masks.Product
import com.tangem.common.CompletionResult
import com.tangem.tasks.file.File
import com.tangem.tasks.file.ReadFilesTask

// TODO: refactor?

class FilesAndDataResponse(
    val cardId: String,
    val walletPublicKey: ByteArray,
    val files: List<File>
) : CommandResponse

class ReadFilesAndDataTask(private val readPrivateFiles: Boolean = false) :
    CardSessionRunnable<FilesAndDataResponse> {

    override val requiresPin2 = readPrivateFiles

    override fun run(
        session: CardSession, callback: (result: CompletionResult<FilesAndDataResponse>) -> Unit
    ) {
        if (session.environment.card?.cardData?.productMask?.contains(Product.IdCard) != true) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
        }
        val command = ReadFilesTask(readPrivateFiles)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val cardId = session.environment.card?.cardId
                    val walletPublicKey = session.environment.card?.walletPublicKey
                    if (cardId == null || walletPublicKey == null) {
                        callback(CompletionResult.Failure(TangemSdkError.CardError()))
                        return@run
                    }
                    callback(
                        CompletionResult.Success(
                            FilesAndDataResponse(cardId, walletPublicKey, result.data.files)
                        )
                    )
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

}