package com.tangem.id.features.home.redux

import com.tangem.TangemError
import com.tangem.id.common.redux.ErrorAction
import org.rekotlin.Action

sealed class HomeAction : Action {
    object ReadCredentialsAsHolder : HomeAction() {
        object Success : HomeAction()
        class Failure(override val error: TangemError) : HomeAction(), ErrorAction
    }
    object ReadCredentialsAsVerifier : HomeAction() {
        object Success : HomeAction()
        class Failure(override val error: TangemError) : HomeAction(), ErrorAction
    }
    object ReadIssuerCard : HomeAction() {
        object Success : HomeAction()
        class Failure(override val error: TangemError) : HomeAction(), ErrorAction
    }
}

