package com.tangem.id.features.issuer.redux

import org.rekotlin.Action

sealed class IssuerAction : Action {
    data class AddAddress(val address: String) : IssuerAction()
    object ReadHoldersCard : IssuerAction()
}

