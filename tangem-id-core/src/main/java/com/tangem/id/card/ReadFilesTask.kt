package com.tangem.id.card

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.commands.CommandResponse
import com.tangem.commands.file.File
import com.tangem.commands.file.ReadFileDataTask
import com.tangem.common.CompletionResult

class FilesResponse(
    val cardId: String,
    val files: List<File>
) : CommandResponse

class ReadFilesTask(private val readPrivateFiles: Boolean = false) :
    CardSessionRunnable<FilesResponse> {

    override val requiresPin2 = readPrivateFiles

    override fun run(
        session: CardSession, callback: (result: CompletionResult<FilesResponse>) -> Unit
    ) {
        val command = ReadFileDataTask(readPrivateFiles)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    callback(
                        CompletionResult.Success(
                            FilesResponse(session.environment.card!!.cardId, result.data.files)
                        )
                    )
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

}