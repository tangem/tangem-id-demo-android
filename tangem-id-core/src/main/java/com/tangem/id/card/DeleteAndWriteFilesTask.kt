package com.tangem.id.card

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemSdkError
import com.tangem.commands.CommandResponse
import com.tangem.commands.file.DeleteFileCommand
import com.tangem.commands.file.FileData
import com.tangem.commands.file.ReadFileDataCommand
import com.tangem.commands.file.WriteFileDataCommand
import com.tangem.common.CompletionResult
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.utils.JsonLdCborEncoder
import com.tangem.tasks.file.File

// TODO: refactor?

class DeleteAndWriteFilesResponse(
    val cardId: String
) : CommandResponse

fun List<File>.toVerifiableCredentials(): List<VerifiableCredential> {
    return this.map { it.fileData }
        .map { JsonLdCborEncoder.decode(it) }
        .map { VerifiableCredential.fromMap((it as Map<String, String>)) }
}

class DeleteAndWriteFilesTask(
    private val data: List<ByteArray>
) : CardSessionRunnable<DeleteAndWriteFilesResponse> {

    override val requiresPin2 = true

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<DeleteAndWriteFilesResponse>) -> Unit
    ) {
        deleteFile(session, callback)
    }

    private fun deleteFile(
        session: CardSession, callback: (result: CompletionResult<DeleteAndWriteFilesResponse>) -> Unit
    ) {
        DeleteFileCommand(0).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> deleteFile(session, callback)
                is CompletionResult.Failure ->
                    if (result.error is TangemSdkError.ErrorProcessingCommand) {
                        getFilesCounter(session, callback)
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
            }

        }

    }

    private fun getFilesCounter(
        session: CardSession, callback: (result: CompletionResult<DeleteAndWriteFilesResponse>) -> Unit
    ) {
        ReadFileDataCommand().run(session) { readResponse ->
            when (readResponse) {
                is CompletionResult.Failure -> callback(CompletionResult.Failure(readResponse.error))
                is CompletionResult.Success -> {
                    val counter = readResponse.data.fileDataCounter ?: 0
                    val cardId = session.environment.card?.cardId
                    if (cardId == null) {
                        callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
                        return@run
                    }
                    if (readResponse.data.fileData.isNotEmpty()) {
                        fixFilesNotDeletedProblem(session, callback)
                        return@run
                    }
                    writeData(0, counter.inc(), cardId, session, callback)
                }
            }
        }
    }

    private fun fixFilesNotDeletedProblem(
        session: CardSession,
        callback: (result: CompletionResult<DeleteAndWriteFilesResponse>) -> Unit
    ) {
        createWriteFileCommand(byteArrayOf(0))
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        deleteFile(session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }

    private fun writeData(
        currentFileIndex: Int, counter: Int,
        cardId: String, session: CardSession,
        callback: (result: CompletionResult<DeleteAndWriteFilesResponse>) -> Unit
    ) {
        if (currentFileIndex > data.lastIndex) {
            callback(CompletionResult.Success(DeleteAndWriteFilesResponse(cardId)))
            return
        }
        val currentFile = data[currentFileIndex]
        createWriteFileCommand(currentFile)
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        writeData(currentFileIndex.inc(), counter.inc(), cardId, session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }

    private fun createWriteFileCommand(
        data: ByteArray
    ): WriteFileDataCommand {
        return WriteFileDataCommand(
            FileData.DataProtectedByPasscode(data)
        )
    }
}