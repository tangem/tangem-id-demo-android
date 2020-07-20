package com.tangem.id.features.issuer.redux

import com.tangem.id.common.redux.AppState
import org.rekotlin.Action

fun issuerReducer(action: Action, state: AppState): IssuerState {

    val issuerAction = action as? IssuerAction ?: return state.issuerState

    when (issuerAction) {
        IssuerAction.NavigateToNewCredentials -> TODO()
    }
}