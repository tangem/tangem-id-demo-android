package com.tangem.id.features.issuecredentials.redux

import com.tangem.id.common.entities.AgeOfMajority
import com.tangem.id.common.entities.Photo
import com.tangem.id.common.extensions.isOver21Years
import com.tangem.id.common.extensions.toDate
import com.tangem.id.common.redux.AppState
import com.tangem.id.tangemIdSdk
import org.rekotlin.Action

fun issueCredentialsReducer(action: Action, state: AppState): IssueCredentialsState {

    if (action !is IssueCredentialsAction) return state.issueCredentialsState

    var newState = state.issueCredentialsState

    when (action) {
        is IssueCredentialsAction.AddPhoto.Success ->
            newState = newState.copy(photo = Photo((action.photo)))
        is IssueCredentialsAction.AddPhoto.Failure -> newState

        is IssueCredentialsAction.SaveInput -> {
            if (newState.holdersAddress == null) return newState

            if (action.passport != null) {
                val passport = newState.passport?.copy(
                    name = action.passport.name ?: newState.passport?.name,
                    surname = action.passport.surname ?: newState.passport?.surname,
                    gender = action.passport.gender ?: newState.passport?.gender,
                    birthDate = action.passport.birthDate ?: newState.passport?.birthDate
                )
                var isOver21 = newState.ageOfMajority?.valid ?: false
                if (action.passport.birthDate != newState.passport?.birthDate) {
                    isOver21 = action.passport.birthDate?.toDate()?.isOver21Years() ?: false
                }
                newState =
                    newState.copy(passport = passport, ageOfMajority = AgeOfMajority(isOver21))
            }

            if (action.ssn != null) {
                val securityNumber = newState.securityNumber?.copy(number = action.ssn)
                newState = newState.copy(securityNumber = securityNumber)
            }
        }

        is IssueCredentialsAction.Sign -> {
            newState = newState.copy(
                editable = false,
                button = IssueCredentialsButton.Sign(enabled = false, progress = true)
            )
        }
        is IssueCredentialsAction.Sign.Success -> {
            newState = newState.copy(
                button = IssueCredentialsButton.WriteCredentials()
            )
        }
        is IssueCredentialsAction.Sign.Cancelled -> {
            newState = newState.copy(
                editable = true,
                button = IssueCredentialsButton.Sign()
            )
        }
        is IssueCredentialsAction.WriteCredentials.Success -> {
            newState = IssueCredentialsState()
        }
        is IssueCredentialsAction.AddHoldersAddress ->
            newState = newState.copy(holdersAddress = action.address)

        is IssueCredentialsAction.ShowJson -> newState =
            newState.copy(jsonShown = tangemIdSdk.issuer.showJsonWhileCreating())
        is IssueCredentialsAction.HideJson -> newState =
            newState.copy(jsonShown = null)

        is IssueCredentialsAction.ResetState -> newState = IssueCredentialsState()

    }
    return newState
}