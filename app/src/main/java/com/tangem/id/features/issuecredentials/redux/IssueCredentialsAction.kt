package com.tangem.id.features.issuecredentials.redux

import android.graphics.Bitmap
import com.tangem.id.common.redux.Gender
import org.rekotlin.Action

sealed class IssueCredentialsAction : Action {
    object AddPhoto : IssueCredentialsAction() {
        data class Success(val photo: Bitmap) : IssueCredentialsAction()
        object Failure : IssueCredentialsAction()
    }
    data class SaveName(val name: String) : IssueCredentialsAction()
    data class SaveSurname(val surname: String) : IssueCredentialsAction()
    data class SaveDate(val date: String) : IssueCredentialsAction()
    data class SaveGender(val gender: Gender) : IssueCredentialsAction()
    data class SaveAgeOfMajority(val ageOfMajority: Boolean) : IssueCredentialsAction()
    data class SaveSecurityNumber(val securityNumber: String) : IssueCredentialsAction()
    data class SaveImmunity(val validUntil: String) : IssueCredentialsAction()
    object Sign : IssueCredentialsAction() {
        object Success : IssueCredentialsAction()
        object Failure : IssueCredentialsAction()
    }
    object WriteCredentials : IssueCredentialsAction() {
        object Success : IssueCredentialsAction()
        object Failure : IssueCredentialsAction()
    }
    object ChangePasscode : IssueCredentialsAction() {
        object Success : IssueCredentialsAction()
        object Failure : IssueCredentialsAction()
    }
}