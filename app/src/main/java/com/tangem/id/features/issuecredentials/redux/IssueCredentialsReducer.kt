package com.tangem.id.features.issuecredentials.redux

import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.Photo
import org.rekotlin.Action

fun issueCredentialsReducer(action: Action, state: AppState): IssueCredentialsState {

    val issueCredentialsAction =
        action as? IssueCredentialsAction ?: return state.issueCredentialsState

    var newState = state.issueCredentialsState
    when (issueCredentialsAction) {
        is IssueCredentialsAction.AddPhoto.Success ->
            newState =
                newState.copy(photo = Photo((issueCredentialsAction.photo)))
        is IssueCredentialsAction.AddPhoto.Failure ->
            newState
        is IssueCredentialsAction.SaveName -> {
            val passport = newState.passport?.copy(name = issueCredentialsAction.name)
            newState = newState.copy(passport = passport)
        }
        is IssueCredentialsAction.SaveSurname -> {
            val passport = newState.passport?.copy(surname = issueCredentialsAction.surname)
            newState = newState.copy(passport = passport)
        }
        is IssueCredentialsAction.SaveDate -> {
            val passport = newState.passport?.copy(birthDate = issueCredentialsAction.date)
            newState = newState.copy(passport = passport)
        }
        is IssueCredentialsAction.SaveGender -> {
            val passport = newState.passport?.copy(gender = issueCredentialsAction.gender)
            newState = newState.copy(passport = passport)
        }
        is IssueCredentialsAction.SaveAgeOfMajority -> {
            val ageOfMajority =
                newState.ageOfMajority?.copy(valid = issueCredentialsAction.ageOfMajority)
            newState = newState.copy(ageOfMajority = ageOfMajority)
        }
        is IssueCredentialsAction.SaveSecurityNumber -> {
            val securityNumber =
                newState.securityNumber?.copy(number = issueCredentialsAction.securityNumber)
            newState = newState.copy(securityNumber = securityNumber)
        }
        is IssueCredentialsAction.Sign -> {
        }
    }
    return newState
}