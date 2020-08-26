package com.tangem.id.features.issuer.redux

import android.os.Handler
import android.os.Looper
import com.tangem.common.CompletionResult
import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.store
import com.tangem.id.tangemIdSdk
import org.rekotlin.Middleware

private val mainThread = Handler(Looper.getMainLooper())

val issuerMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is IssuerAction.ReadHoldersCard -> {
                    tangemIdSdk.issuer.getHolderAddress { result ->
                        mainThread.post {
                            store.dispatch(IssueCredentialsAction.ResetState)
                            when (result) {
                                is CompletionResult.Success -> {
                                    store.dispatch(IssueCredentialsAction.AddHoldersAddress(result.data))
                                    store.dispatch(NavigationAction.NavigateTo(AppScreen.IssueCredentials))
                                }
                                is CompletionResult.Failure ->
                                    store.dispatch(IssuerAction.ReadHoldersCard.Failure(result.error))
                            }
                        }
                    }
                }
            }
            next(action)
        }
    }
}