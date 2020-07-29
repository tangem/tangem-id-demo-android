package com.tangem.id.features.issuecredentials.redux

import android.content.Context
import android.graphics.Bitmap
import com.tangem.id.common.redux.Gender
import org.rekotlin.Action

sealed class IssueCredentialsAction : Action {
    object AddPhoto : IssueCredentialsAction() {
        data class Success(val photo: Bitmap) : IssueCredentialsAction()
        object Failure : IssueCredentialsAction()
    }
    data class AddHoldersAddress(val address: String): IssueCredentialsAction()

    data class SavePersonalInfo(
        val name: String?, val surname: String?, val gender: Gender?, val date: String?
    ) : IssueCredentialsAction()
//    data class SaveGender(val gender: Gender) : IssueCredentialsAction()

    data class SaveSecurityNumber(val securityNumber: String) : IssueCredentialsAction()

    data class Sign(val context: Context) : IssueCredentialsAction() {
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