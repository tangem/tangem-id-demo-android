package com.tangem.id.features.home

import org.rekotlin.Action

sealed class HomeAction : Action {
    object ReadCredentialsAsHolder : HomeAction() {
        object Success : HomeAction()
        object Failure : HomeAction()
    }
    object ReadCredentialsAsVerifier : HomeAction() {
        object Success : HomeAction()
        object Failure : HomeAction()
    }
    object ReadIssuerCard : HomeAction() {
        object Success : HomeAction()
        object Failure : HomeAction()
    }
}

