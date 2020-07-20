package com.tangem.id.common.redux

import com.tangem.id.common.redux.navigation.navigationReducer
import com.tangem.id.features.holder.redux.holderReducer
import com.tangem.id.features.issuecredentials.redux.issueCredentialsReducer
import com.tangem.id.features.issuer.redux.issuerReducer
import com.tangem.id.features.verifier.redux.verifierReducer
import org.rekotlin.Action

fun appReducer(action: Action, state: AppState?): AppState {
    requireNotNull(state)
    if (action is AppAction.RestoreState) return action.state
    return AppState(
        navigationState = navigationReducer(action, state),
        issuerState = issuerReducer(action, state),
        issueCredentialsState = issueCredentialsReducer(action, state),
        holderState = holderReducer(action, state),
        verifierState = verifierReducer(action, state)
    )
}

sealed class AppAction : Action {
    data class RestoreState(val state: AppState) : AppAction()
}