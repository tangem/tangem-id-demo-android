package com.tangem.id.card

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.commands.SimpleResponse
import com.tangem.commands.file.File
import com.tangem.commands.file.FileSettings
import com.tangem.common.CompletionResult

class ChangeFilesTask(
    private val filesToDelete: List<File>,
    private val filesToChangeVisibility: List<File>
) : CardSessionRunnable<SimpleResponse> {

    override val requiresPin2 = true

    override fun run(
        session: CardSession, callback: (result: CompletionResult<SimpleResponse>) -> Unit
    ) {
        ChangeFilesSettingsTask(filesToChangeVisibility).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> DeleteFilesTask(filesToDelete).run(session, callback)
                is CompletionResult.Failure -> callback(result)
            }
        }
    }
}

fun FileSettings.toggleVisibility(): FileSettings {
    return if (this == FileSettings.Public) FileSettings.Private else FileSettings.Public
}