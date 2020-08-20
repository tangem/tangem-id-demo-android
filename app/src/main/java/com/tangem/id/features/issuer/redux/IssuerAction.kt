package com.tangem.id.features.issuer.redux

import com.tangem.TangemError
import com.tangem.id.common.redux.ErrorAction
import org.rekotlin.Action

sealed class IssuerAction : Action {
    data class AddAddress(val address: String) : IssuerAction()
    object ReadHoldersCard : IssuerAction() {
        class Failure(override val error: TangemError) : IssuerAction(), ErrorAction
    }
}

