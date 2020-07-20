package com.tangem.id.features.holder.redux

import com.tangem.id.common.redux.*
import org.rekotlin.Action

fun holderReducer(action: Action, state: AppState): HolderState {

    val holderAction = action as? HolderAction ?: return state.holderState

    var newState = state.holderState

    when (holderAction) {
        HolderAction.ToggleEditCredentials -> {
            val editActivated = newState.editActivated

            if (editActivated && newState.credentialsToDelete.isNotEmpty()) {
                for (credential in newState.credentialsToDelete) {
                    when (credential) {
                        is Photo -> {
                            newState = newState.copy(photo = credential)
                        }
                        is Passport -> {
                            newState = newState.copy(passport = credential)
                        }
                        is SecurityNumber -> {
                            newState = newState.copy(securityNumber = credential)
                        }
                        is AgeOfMajority -> {
                            newState = newState.copy(ageOfMajority = credential)
                        }
                        is ImmunityPassport -> {
                            newState = newState.copy(immunityPassport = credential)
                        }
                    }
                }
            }

            if (editActivated) newState = newState.copy(accessLevelsModified = newState.accessLevelsFromCard)

            newState = newState.copy(editActivated = !editActivated, credentialsToDelete = arrayListOf())
        }
        is HolderAction.RequestNewCredential.Success -> {
            newState = newState.copy(immunityPassport = holderAction.immunityPassport)
        }
        HolderAction.SaveChanges -> {

        }
        HolderAction.SaveChanges.Success -> {

        }
        HolderAction.SaveChanges.Failure -> {

        }
        is HolderAction.ChangeCredentialAccessLevel -> {
            if (newState.editActivated) {
                val credentialsAccessLevel =
                    newState.accessLevelsModified.toggleAccessLevel(holderAction.credential)
                newState = newState.copy(accessLevelsModified = credentialsAccessLevel)
            }

        }
        is HolderAction.RemoveCredential -> {
            if (newState.editActivated) {
                val credentialsToDelete = newState.credentialsToDelete + holderAction.credential
                when (holderAction.credential) {
                    is Photo -> {
                        newState = newState.copy(
                            photo = null,
                            credentialsToDelete = ArrayList(credentialsToDelete)
                        )
                    }
                    is Passport -> {
                        newState = newState.copy(
                            passport = null,
                            credentialsToDelete = ArrayList(credentialsToDelete)
                        )
                    }
                    is SecurityNumber -> {
                        newState = newState.copy(
                            securityNumber = null,
                            credentialsToDelete = ArrayList(credentialsToDelete)
                        )
                    }
                    is AgeOfMajority -> {
                        newState = newState.copy(
                            ageOfMajority = null,
                            credentialsToDelete = ArrayList(credentialsToDelete)
                        )
                    }
                    is ImmunityPassport -> {
                        newState = newState.copy(
                            immunityPassport = null,
                            credentialsToDelete = ArrayList(credentialsToDelete)
                        )
                    }
                }
            }
        }
        is HolderAction.ShowCredentialDetails -> {
            newState = newState.copy(detailsOpened = holderAction.credential)
        }
        is HolderAction.ShowJson -> {

        }
    }
    return newState
}


