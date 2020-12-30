package com.tangem.id.features.verifier.redux

import com.tangem.id.common.entities.*
import com.tangem.id.common.redux.AppState
import com.tangem.id.tangemIdSdk
import org.rekotlin.Action

fun verifierReducer(action: Action, state: AppState): VerifierState {

    if (action !is VerifierAction) return state.verifierState

    var newState = state.verifierState

    when (action) {
        is VerifierAction.CredentialsRead -> {
            newState = VerifierState(
                passport = action.credentials.find { it.credential is Passport }
                        as? VerifierCredential<Passport>,
                photo = action.credentials.find { it.credential is Photo }
                        as? VerifierCredential<Photo>,
                securityNumber = action.credentials.find { it.credential is SecurityNumber }
                        as? VerifierCredential<SecurityNumber>,
                ageOfMajority = action.credentials.find { it.credential is AgeOfMajority }
                        as? VerifierCredential<AgeOfMajority>,
                immunityPassport = action.credentials.find { it.credential is ImmunityPassport }
                        as? VerifierCredential<ImmunityPassport>,
                credentialNinja = action.credentials.find { it.credential is CredentialNinja }
                        as? VerifierCredential<CredentialNinja>
            )
        }
        is VerifierAction.ToggleIssuerStatus -> TODO()
        VerifierAction.ShowJson -> {
            newState = newState.copy(jsonShown = tangemIdSdk.verifier.showRawVerifierCredential())
        }
        VerifierAction.HideJson -> {
            newState = newState.copy(jsonShown = null)
        }
    }
    return newState
}