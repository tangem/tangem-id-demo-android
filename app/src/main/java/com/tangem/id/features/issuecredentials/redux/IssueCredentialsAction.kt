package com.tangem.id.features.issuecredentials.redux

import android.content.Context
import android.graphics.Bitmap
import com.tangem.TangemError
import com.tangem.id.R
import com.tangem.id.common.redux.ErrorAction
import com.tangem.id.common.redux.Gender
import com.tangem.id.common.redux.NotificationAction
import org.rekotlin.Action

sealed class IssueCredentialsAction : Action {
    object AddPhoto : IssueCredentialsAction() {
        data class Success(val photo: Bitmap) : IssueCredentialsAction()
        object Failure : IssueCredentialsAction()
    }

    data class AddHoldersAddress(val address: String) : IssueCredentialsAction()

    data class SavePersonalInfo(
        val name: String?, val surname: String?, val gender: Gender?, val date: String?
    ) : IssueCredentialsAction()
//    data class SaveGender(val gender: Gender) : IssueCredentialsAction()

    data class SaveSecurityNumber(val securityNumber: String) : IssueCredentialsAction()

    data class Sign(val context: Context) : IssueCredentialsAction() {
        object Success : IssueCredentialsAction(), NotificationAction {
            override val messageResource = R.string.issue_credentials_notification_sign_success
        }

        class Failure(override val error: TangemError) : IssueCredentialsAction(), ErrorAction
    }

    object WriteCredentials : IssueCredentialsAction() {
        class Success() : IssueCredentialsAction(), NotificationAction {
            override val messageResource = R.string.issue_credentials_notification_write_success
        }

        class Failure(override val error: TangemError) : IssueCredentialsAction(), ErrorAction
    }

    object ResetState : IssueCredentialsAction()

    object ShowJson : IssueCredentialsAction()
    object HideJson : IssueCredentialsAction()
}