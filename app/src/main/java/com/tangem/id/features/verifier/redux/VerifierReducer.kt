package com.tangem.id.features.verifier.redux

import com.tangem.id.common.redux.AppState
import org.rekotlin.Action

fun verifierReducer(action: Action, state: AppState): VerifierState {

    val verifierAction = action as? VerifierAction ?: return state.verifierState

    when (verifierAction) {
        is VerifierAction.ToggleIssuerStatus -> TODO()
        VerifierAction.ShowJson -> TODO()
    }
}