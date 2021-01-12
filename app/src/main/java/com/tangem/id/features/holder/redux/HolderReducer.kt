package com.tangem.id.features.holder.redux

import com.tangem.id.common.redux.AppState
import com.tangem.id.tangemIdSdk
import org.rekotlin.Action

fun holderReducer(action: Action, state: AppState): HolderState {

    if (action !is HolderAction) return state.holderState

    var newState = state.holderState

    when (action) {
        is HolderAction.CredentialsRead -> {
            newState = HolderState(
                cardId = action.cardId,
                credentials = action.credentials,
                credentialsOnCard = action.credentials
            )
        }

        HolderAction.ToggleEditCredentials -> {
            val editActivated = newState.editActivated

            newState =
                newState.copy(
                    editActivated = !editActivated,
                    credentials = newState.credentialsOnCard,
                    credentialsToDelete = listOf()
                )
        }
        is HolderAction.RequestNewCredential.Success -> {
            newState = newState.copy(
                credentials = action.allCredentials,
                credentialsOnCard = action.allCredentials
            )
        }
        HolderAction.SaveChanges -> {
            if (newState.credentialsOnCard == newState.credentials) {
                newState = newState.copy(editActivated = false)
            }
        }
        HolderAction.SaveChanges.Success -> {
            newState = newState.copy(
                editActivated = false,
                credentialsOnCard = newState.credentials,
                credentialsToDelete = listOf()
            )
        }
        is HolderAction.SaveChanges.Failure -> {
            newState = newState.copy(
                editActivated = false,
                credentials = newState.credentialsOnCard,
                credentialsToDelete = listOf()
            )
        }
        is HolderAction.ChangeCredentialAccessLevel -> {
            if (newState.editActivated) {
                val editedCredentials = newState.credentials
                    .map {
                        if (it.credential == action.credential) {
                            it.toggleVisibility()
                        } else {
                            it
                        }
                    }
                newState = newState.copy(credentials = editedCredentials)
            }

        }
        is HolderAction.RemoveCredential -> {
            if (newState.editActivated) {
                val credentialToDelete =
                    newState.credentials.find { it.credential == action.credential }
                if (credentialToDelete != null) {
                    val editedCredentials =
                        newState.credentials.filter { it != credentialToDelete }
                    newState = newState.copy(
                        credentials = editedCredentials,
                        credentialsToDelete = newState.credentialsToDelete + credentialToDelete.file
                    )
                }
            }
        }
        is HolderAction.ChangePasscodeAction -> {
            newState = newState.copy(
                    editActivated = false, credentials = newState.credentialsOnCard,
                    credentialsToDelete = listOf()
                )
        }
        is HolderAction.ShowCredentialDetails -> {
            newState = newState.copy(detailsOpened = action.credential)
        }
        HolderAction.HideCredentialDetails -> {
            newState = newState.copy(detailsOpened = null, rawCredentialShown = null)
        }
        is HolderAction.ShowRawCredential -> {
            val index =
                newState.credentialsOnCard.indexOfFirst { it.credential == action.credential }
            val json = tangemIdSdk.holder.showRawHoldersCredential(index)
            newState = newState.copy(rawCredentialShown = json)

        }
    }
    return newState
}


