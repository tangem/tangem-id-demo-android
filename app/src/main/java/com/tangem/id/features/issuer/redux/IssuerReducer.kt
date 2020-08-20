package com.tangem.id.features.issuer.redux

import com.tangem.id.common.redux.AppState
import org.rekotlin.Action

fun issuerReducer(action: Action, state: AppState): IssuerState {
    if (action !is IssuerAction) return state.issuerState

    return when (action) {
        is IssuerAction.AddAddress -> state.issuerState.copy(issuerAddress = action.address)
        is IssuerAction.ReadHoldersCard -> {
            state.issuerState
        }
        is IssuerAction.ReadHoldersCard.Failure -> {
            state.issuerState
        }
    }
}