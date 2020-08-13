package com.tangem.id.features.holder.redux

import com.tangem.commands.file.FileSettings
import com.tangem.id.common.entities.Button
import com.tangem.id.common.entities.Credential
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

sealed class HolderScreenButton(enabled: Boolean) : Button(enabled) {
    class RequestNewCredential(enabled: Boolean = true) : HolderScreenButton(enabled)
    class SaveChanges(enabled: Boolean = true) : HolderScreenButton(enabled)
}

data class HolderState(
    val cardId: String? = null,
    val editActivated: Boolean = false,
    val detailsOpened: Credential? = null,
    val jsonShown: String? = null,
    val credentials: List<Pair<Credential, AccessLevel>> = listOf(),
    val credentialsOnCard: List<Pair<Credential, AccessLevel>> = listOf(),
    val credentialsToDelete: List<Credential> = listOf()
) : StateType {

    val button: HolderScreenButton = if (editActivated) {
        HolderScreenButton.SaveChanges()
    } else {
        HolderScreenButton.RequestNewCredential()
    }
}

