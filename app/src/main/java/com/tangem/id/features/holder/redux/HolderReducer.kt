package com.tangem.id.features.holder.redux

import com.tangem.id.common.redux.AppState
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
                    credentials = newState.credentialsOnCard
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
        HolderAction.SaveChanges.Failure -> {
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
                        if (it.first == action.credential) {
                            it.first to it.second.toggleVisibility()
                        } else {
                            it
                        }
                    }
                newState = newState.copy(credentials = editedCredentials)
            }

        }
        is HolderAction.RemoveCredential -> {
            if (newState.editActivated) {
                val editedCredentials =
                    newState.credentials.filter { it.first != action.credential }
                newState = newState.copy(
                    credentials = editedCredentials,
                    credentialsToDelete = newState.credentialsToDelete + action.credential
                )
            }
        }
        is HolderAction.ShowCredentialDetails -> {
            newState = newState.copy(detailsOpened = action.credential)
        }
        HolderAction.HideCredentialDetails -> {
            newState = newState.copy(detailsOpened = null)
        }

        is HolderAction.ShowJson -> {

        }
    }
    return newState
}


