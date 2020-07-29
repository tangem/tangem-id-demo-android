package com.tangem.id.features.holder.redux

import com.tangem.id.common.redux.*
import org.rekotlin.Action

fun holderReducer(action: Action, state: AppState): HolderState {

    if (action !is HolderAction) return state.holderState

    var newState = state.holderState

    when (action) {
        is HolderAction.CredentialsRead -> {
            newState = HolderState(
                cardId = action.cardId,
                accessLevelsFromCard = action.accessLevels,
                passport = action.credentials.find { it is Passport } as? Passport,
                photo = action.credentials.find { it is Photo } as? Photo,
                securityNumber = action.credentials.find { it is SecurityNumber } as? SecurityNumber,
                ageOfMajority = action.credentials.find { it is AgeOfMajority } as? AgeOfMajority
            )
        }

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

            if (editActivated) newState =
                newState.copy(accessLevelsModified = newState.accessLevelsFromCard)

            newState =
                newState.copy(editActivated = !editActivated, credentialsToDelete = arrayListOf())
        }
        is HolderAction.RequestNewCredential.Success -> {
            newState = newState.copy(immunityPassport = action.immunityPassport)
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
                    newState.accessLevelsModified.toggleAccessLevel(action.credential)
                newState = newState.copy(accessLevelsModified = credentialsAccessLevel)
            }

        }
        is HolderAction.RemoveCredential -> {
            if (newState.editActivated) {
                val credentialsToDelete = newState.credentialsToDelete + action.credential
                when (action.credential) {
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
            newState = newState.copy(detailsOpened = action.credential)
        }
        is HolderAction.ShowJson -> {

        }
    }
    return newState
}

