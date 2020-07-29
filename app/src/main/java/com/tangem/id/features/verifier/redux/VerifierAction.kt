package com.tangem.id.features.verifier.redux

import org.rekotlin.Action

sealed class VerifierAction : Action {
    data class ToggleIssuerStatus(val issuer: Issuer) : VerifierAction()
    data class CredentialsRead(val credentials: List<VerifierCredential<*>>) : VerifierAction()
    object ShowJson : VerifierAction()
}
