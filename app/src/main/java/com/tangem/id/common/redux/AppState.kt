package com.tangem.id.common.redux

import com.tangem.id.common.redux.navigation.NavigationState
import com.tangem.id.common.redux.navigation.navigationMiddleware
import com.tangem.id.features.holder.redux.HolderState
import com.tangem.id.features.holder.redux.holderMiddleware
import com.tangem.id.features.holder.ui.RequestCredentialsState
import com.tangem.id.features.home.redux.homeMiddleware
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsState
import com.tangem.id.features.issuecredentials.redux.issueCredentialsMiddleware
import com.tangem.id.features.issuer.redux.IssuerState
import com.tangem.id.features.issuer.redux.issuerMiddleware
import com.tangem.id.features.verifier.redux.VerifierState
import org.rekotlin.Middleware
import org.rekotlin.StateType

data class AppState(
    val navigationState: NavigationState = NavigationState(),
    val issuerState: IssuerState = IssuerState(),
    val issueCredentialsState: IssueCredentialsState = IssueCredentialsState(),
    val holderState: HolderState = HolderState(),
    val verifierState: VerifierState = VerifierState(),
    val requestCredentialsState: RequestCredentialsState = RequestCredentialsState()
) : StateType {

    companion object {
        fun getMiddleware(): List<Middleware<AppState>> {
            return listOf(
                navigationMiddleware, notificationsMiddleware,
                holderMiddleware, homeMiddleware, issuerMiddleware, issueCredentialsMiddleware
            )
        }
    }
}


