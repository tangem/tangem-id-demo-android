package com.tangem.id.features.issuecredentials.redux

import android.content.Context
import android.graphics.Bitmap
import com.tangem.TangemError
import com.tangem.id.R
import com.tangem.id.common.entities.Passport
import com.tangem.id.common.redux.ErrorAction
import com.tangem.id.common.redux.NotificationAction
import org.rekotlin.Action

sealed class IssueCredentialsAction : Action {
    object AddPhoto : IssueCredentialsAction() {
        data class Success(val photo: Bitmap) : IssueCredentialsAction()
        object Failure : IssueCredentialsAction()
    }

    object NoCameraPermission : IssueCredentialsAction(), NotificationAction {
        override val messageResource = R.string.issue_credentials_notification_no_camera_permission
    }

    object FormIncomplete : IssueCredentialsAction(), NotificationAction {
        override val messageResource = R.string.issue_credentials_notification_incomplete_form
    }

    data class AddHoldersAddress(val address: String) : IssueCredentialsAction()

    data class SaveInput(val passport: Passport? = null, val ssn: String? = null) : IssueCredentialsAction()

    data class Sign(val context: Context) : IssueCredentialsAction() {
        object Success : IssueCredentialsAction(), NotificationAction {
            override val messageResource = R.string.issue_credentials_notification_sign_success
        }

        class Failure(override val error: TangemError) : IssueCredentialsAction(), ErrorAction
        object Cancelled : IssueCredentialsAction()
    }

    object WriteCredentials : IssueCredentialsAction() {
        class Success() : IssueCredentialsAction(), NotificationAction {
            override val messageResource = R.string.issue_credentials_notification_write_success
        }
        object Cancelled : IssueCredentialsAction()
        class Failure(override val error: TangemError) : IssueCredentialsAction(), ErrorAction
    }

    object ResetState : IssueCredentialsAction()

    object ShowJson : IssueCredentialsAction()
    object HideJson : IssueCredentialsAction()
}