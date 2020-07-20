package com.tangem.id.features.verifier.redux

import org.rekotlin.Action

sealed class VerifierAction : Action {
    data class ToggleIssuerStatus(val issuer: Issuer) : VerifierAction()
    object ShowJson : VerifierAction()
}
