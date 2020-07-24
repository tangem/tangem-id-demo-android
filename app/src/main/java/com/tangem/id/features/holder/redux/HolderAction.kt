package com.tangem.id.features.holder.redux

import com.tangem.id.common.redux.Credential
import com.tangem.id.common.redux.ImmunityPassport
import org.rekotlin.Action

sealed class HolderAction : Action {
    object ToggleEditCredentials : HolderAction()
    object RequestNewCredential : HolderAction() {
        data class Success(val immunityPassport: ImmunityPassport) : HolderAction()
        object Failure : HolderAction()
    }
    object SaveChanges : HolderAction() {
        object Success : HolderAction()
        object Failure : HolderAction()
    }
    data class ChangeCredentialAccessLevel(val credential: Credential) : HolderAction()
    data class RemoveCredential(val credential: Credential) : HolderAction()
    data class ShowCredentialDetails(val credential: Credential?) : HolderAction()
    data class ShowJson(val credential: Credential) : HolderAction()
}

