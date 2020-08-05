package com.tangem.id.features.issuecredentials.redux

import com.tangem.id.common.extensions.isOver21Years
import com.tangem.id.common.extensions.toDate
import com.tangem.id.common.redux.AgeOfMajority
import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.Photo
import org.rekotlin.Action

fun issueCredentialsReducer(action: Action, state: AppState): IssueCredentialsState {

    if (action !is IssueCredentialsAction) return state.issueCredentialsState

    var newState = state.issueCredentialsState
    when (action) {
        is IssueCredentialsAction.AddPhoto.Success ->
            newState = newState.copy(photo = Photo((action.photo)))
        is IssueCredentialsAction.AddPhoto.Failure -> newState
        is IssueCredentialsAction.SavePersonalInfo -> {
            val passport = newState.passport?.copy(
                name = action.name ?: newState.passport?.name,
                surname = action.surname ?: newState.passport?.surname,
                gender = action.gender ?: newState.passport?.gender,
                birthDate = action.date ?: newState.passport?.birthDate
            )
            var isOver21 = newState.ageOfMajority?.valid ?: false
            if (action.date != newState.passport?.birthDate) {
                isOver21 = action.date?.toDate()?.isOver21Years() ?: false
            }
            newState = newState.copy(passport = passport, ageOfMajority = AgeOfMajority(isOver21))
        }
//        is IssueCredentialsAction.SaveGender -> {
//            val passport = newState.passport?.copy(
//                gender = action.gender
//            )
//            newState = newState.copy(passport = passport)
//        }
        is IssueCredentialsAction.SaveSecurityNumber -> {
            val securityNumber =
                newState.securityNumber?.copy(number = action.securityNumber)
            newState = newState.copy(securityNumber = securityNumber)
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
        is IssueCredentialsAction.WriteCredentials.Success -> {
            newState = IssueCredentialsState()
        }
        is IssueCredentialsAction.AddHoldersAddress ->
            newState = newState.copy(holdersAddress = action.address)
        is IssueCredentialsAction.ResetState -> newState = IssueCredentialsState()
    }
    return newState
}