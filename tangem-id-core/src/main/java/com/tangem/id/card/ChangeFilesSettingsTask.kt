package com.tangem.id.card

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.commands.SimpleResponse
import com.tangem.commands.file.ChangeFileSettingsCommand
import com.tangem.commands.file.FileSettingsChange
import com.tangem.common.CompletionResult
import com.tangem.tasks.file.File
import java.util.*

class ChangeFilesSettingsTask(
    filesToChangeVisibility: List<File>
) : CardSessionRunnable<SimpleResponse> {

    override val requiresPin2 = true
    private val filesToChangeVisibility = LinkedList(filesToChangeVisibility)

    override fun run(
        session: CardSession, callback: (result: CompletionResult<SimpleResponse>) -> Unit
    ) {
        changeVisibility(session, callback)
    }

    private fun changeVisibility(
        session: CardSession,
        callback: (result: CompletionResult<SimpleResponse>) -> Unit
    ) {
        val file = filesToChangeVisibility.pollFirst()
        if (file == null) {
            callback(
                CompletionResult.Success(
                    SimpleResponse(session.environment.card?.cardId ?: "")
                )
            )
            return
        }
        val command =
            ChangeFileSettingsCommand(FileSettingsChange(file.fileIndex, file.fileSettings!!))
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    changeVisibility(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
