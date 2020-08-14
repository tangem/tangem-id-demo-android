package com.tangem.id.card

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.commands.SimpleResponse
import com.tangem.commands.file.ChangeFileSettingsCommand
import com.tangem.commands.file.DeleteFileCommand
import com.tangem.commands.file.FileSettings
import com.tangem.common.CompletionResult
import java.util.*

class ChangeFilesTask(
    indicesToDelete: List<Int>,
    indicesToChangeVisibility: List<Int>,
    currentVisibility: List<FileSettings>
) : CardSessionRunnable<SimpleResponse> {

    override val requiresPin2 = true
    private val filesToDelete = TreeSet(indicesToDelete)
    private val filesToChangeVisibility = LinkedList(indicesToChangeVisibility)
    private val visibilities = LinkedList(
        currentVisibility.map { it.toggleVisibility() }
    )

    override fun run(
        session: CardSession, callback: (result: CompletionResult<SimpleResponse>) -> Unit
    ) {
        changeVisibility(session, callback)
    }

    private fun changeVisibility(
        session: CardSession,
        callback: (result: CompletionResult<SimpleResponse>) -> Unit
    ) {
        val settings = visibilities.pollFirst()
        val index = filesToChangeVisibility.pollFirst()
        if (index == null || settings == null) {
            deleteFile(session, callback)
            return
        }
        val command = ChangeFileSettingsCommand(settings, index)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    changeVisibility(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun deleteFile(
        session: CardSession,
        callback: (result: CompletionResult<SimpleResponse>) -> Unit
    ) {
        val index = filesToDelete.pollLast()
        if (index == null) {
            callback(
                CompletionResult.Success(
                    SimpleResponse(session.environment.card?.cardId ?: "")
                )
            )
            return
        }
        val command = DeleteFileCommand(index)
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

fun FileSettings.toggleVisibility(): FileSettings {
    return if (this == FileSettings.Public) FileSettings.Private else FileSettings.Public
}