package com.tangem.id.features.holder.redux

import com.tangem.TangemError
import com.tangem.id.R
import com.tangem.id.common.entities.Credential
import com.tangem.id.common.redux.ErrorAction
import com.tangem.id.common.redux.NotificationAction
import org.rekotlin.Action

sealed class HolderAction : Action {
    object ToggleEditCredentials : HolderAction()

    data class CredentialsRead(
        val cardId: String,
        val walletPublicKey: ByteArray,
        val credentials: List<HolderCredential>
    ) : HolderAction()

    data class RequestNewCredential(val requestUri: String) : HolderAction() {
        data class Success(val allCredentials: List<HolderCredential>) :
            HolderAction(), NotificationAction {
            override val messageResource = R.string.holder_screen_notification_request_credential_success
        }
        class Failure(override val error: TangemError) : HolderAction(), ErrorAction
    }

    object NoCameraPermission : HolderAction(), NotificationAction {
        override val messageResource = R.string.holder_screen_notification_no_camera_permission
    }

    object SaveChanges : HolderAction() {
        object Success : HolderAction(), NotificationAction {
            override val messageResource = R.string.holder_screen_notification_save_changes_success
        }

        class Failure(override val error: TangemError) : HolderAction(), ErrorAction
    }

    object ChangePasscodeAction : HolderAction() {
        object Success : HolderAction(), NotificationAction {
            override val messageResource =
                R.string.holder_screen_notification_change_passcode_success
        }

        class Failure(override val error: TangemError) : HolderAction(), ErrorAction
    }

    data class ChangeCredentialAccessLevel(val credential: Credential) : HolderAction()
    data class RemoveCredential(val credential: Credential) : HolderAction()
    data class ShowCredentialDetails(val credential: Credential) : HolderAction()
    object HideCredentialDetails : HolderAction()
    data class ShowRawCredential(val credential: Credential) : HolderAction()
}

