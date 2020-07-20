package com.tangem.id.features.issuer.redux

import org.rekotlin.Action

sealed class IssuerAction : Action {
    object NavigateToNewCredentials : IssuerAction()
}

