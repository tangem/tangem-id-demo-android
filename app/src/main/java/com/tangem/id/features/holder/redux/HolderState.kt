package com.tangem.id.features.holder.redux

import com.tangem.commands.file.FileSettings
import com.tangem.id.card.toggleVisibility
import com.tangem.id.common.entities.Button
import com.tangem.id.common.entities.Credential
import com.tangem.id.demo.HolderDemoCredential
import com.tangem.tasks.file.File
import org.rekotlin.StateType

enum class AccessLevel {
    Private, Public;

    fun toggleVisibility(): AccessLevel {
        return when (this) {
            Private -> Public
            Public -> Private
        }
    }

    companion object {
        fun from(settings: FileSettings): AccessLevel {
            return if (settings == FileSettings.Public) Public else Private
        }
    }
}

data class HolderCredential(
    val credential: Credential,
    val accessLevel: AccessLevel,
    val file: File
) {
    fun toggleVisibility(): HolderCredential {
        return this.copy(
            accessLevel = accessLevel.toggleVisibility(),
            file = file.copy(fileSettings = file.fileSettings?.toggleVisibility())
        )
    }
}

fun HolderDemoCredential.toHolderCredential(): HolderCredential {
    return HolderCredential(
        Credential.from(this.demoCredential),
        AccessLevel.from(this.file.fileSettings!!),
        this.file
    )
}

sealed class HolderScreenButton(enabled: Boolean) : Button(enabled) {
    class RequestNewCredential(enabled: Boolean = true) : HolderScreenButton(enabled)
    class SaveChanges(enabled: Boolean = true) : HolderScreenButton(enabled)
}

data class HolderState(
    val cardId: String? = null,
    val editActivated: Boolean = false,
    val detailsOpened: Credential? = null,
    val jsonShown: String? = null,
    val credentials: List<HolderCredential> = listOf(),
    val credentialsOnCard: List<HolderCredential> = listOf(),
    val credentialsToDelete: List<File> = listOf()
) : StateType {

    val button: HolderScreenButton = if (editActivated) {
        HolderScreenButton.SaveChanges()
    } else {
        HolderScreenButton.RequestNewCredential()
    }

    fun getFilesToChangeVisibility(): List<File> {
        return credentials.filter { credential ->
            val credentialOnCard = credentialsOnCard.find {
                it.file.fileIndex == credential.file.fileIndex
            }
            credential.file.fileSettings != credentialOnCard?.file?.fileSettings
        }.map { it.file }
    }
}

