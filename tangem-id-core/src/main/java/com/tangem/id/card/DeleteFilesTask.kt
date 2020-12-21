package com.tangem.id.card

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.commands.SimpleResponse
import com.tangem.commands.file.DeleteFileCommand
import com.tangem.common.CompletionResult
import com.tangem.tasks.file.File
import java.util.*

class DeleteFilesTask(
    filesToDelete: List<File>
) : CardSessionRunnable<SimpleResponse> {

    override val requiresPin2 = true
    private val filesToDelete = LinkedList(filesToDelete.sortedBy { it.fileIndex })

    override fun run(
        session: CardSession, callback: (result: CompletionResult<SimpleResponse>) -> Unit
    ) {
        deleteFile(session, callback)
    }

    private fun deleteFile(
        session: CardSession,
        callback: (result: CompletionResult<SimpleResponse>) -> Unit
    ) {
        val file = filesToDelete.pollLast()
        if (file == null) {
            callback(
                CompletionResult.Success(
                    SimpleResponse(session.environment.card?.cardId ?: "")
                )
            )
            return
        }
        val command = DeleteFileCommand(file.fileIndex)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    deleteFile(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
