package com.tangem.id.features.home.redux

import android.os.Handler
import android.os.Looper
import com.tangem.common.CompletionResult
import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.holder.redux.HolderAction
import com.tangem.id.features.holder.redux.toHolderCredential
import com.tangem.id.features.issuer.redux.IssuerAction
import com.tangem.id.features.verifier.redux.VerifierAction
import com.tangem.id.features.verifier.redux.VerifierCredential
import com.tangem.id.store
import com.tangem.id.tangemIdSdk
import org.rekotlin.Middleware

private val mainThread = Handler(Looper.getMainLooper())

val homeMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is HomeAction.ReadIssuerCard -> {
                    tangemIdSdk.issuer.readIssuerCard { result ->
                        mainThread.post {
                            when (result) {
                                is CompletionResult.Success -> {
                                    store.dispatch(IssuerAction.AddAddress(result.data))
                                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Issuer))
                                }
                                is CompletionResult.Failure ->
                                    store.dispatch(HomeAction.ReadIssuerCard.Failure(result.error))
                            }
                        }
                    }
                }
                is HomeAction.ReadCredentialsAsVerifier -> {
                    tangemIdSdk.verifier.readCredentialsAsVerifier { result ->
                        when (result) {
                            is CompletionResult.Success -> {
                                val verifierCredentials =
                                    result.data.mapNotNull { VerifierCredential.from(it) }
                                mainThread.post {
                                    store.dispatch(
                                        VerifierAction.CredentialsRead(
                                            verifierCredentials
                                        )
                                    )
                                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Verifier))
                                }
                            }
                            is CompletionResult.Failure ->
                                store.dispatch(
                                    HomeAction.ReadCredentialsAsVerifier.Failure(result.error)
                                )
                        }
                    }
                }
                is HomeAction.ReadCredentialsAsHolder -> {
                    tangemIdSdk.holder.readCredentialsAsHolder { result ->
                        mainThread.post {
                            when (result) {
                                is CompletionResult.Failure ->
                                    store.dispatch(HomeAction.ReadCredentialsAsHolder.Failure(result.error))
                                is CompletionResult.Success -> {
                                    val holdersCredentials =
                                        result.data.credentials.map { it.toHolderCredential() }

                                    store.dispatch(
                                        HolderAction.CredentialsRead(
                                            result.data.cardId,
                                            result.data.walletPublicKey,
                                            holdersCredentials
                                        )
                                    )
                                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Holder))
                                }

                            }
                        }

                    }
                }
            }
            next(action)
        }
    }
}