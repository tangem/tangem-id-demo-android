package com.tangem.id.features.holder.redux

import android.os.Handler
import android.os.Looper
import com.tangem.commands.file.File
import com.tangem.common.CompletionResult
import com.tangem.id.SimpleResponse
import com.tangem.id.common.redux.AppState
import com.tangem.id.store
import com.tangem.id.tangemIdSdk
import org.rekotlin.Middleware

private val mainThread = Handler(Looper.getMainLooper())

val holderMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is HolderAction.SaveChanges -> {
                    val credentialsOnCard = store.state.holderState.credentialsOnCard
                    val credentialsWithChanges = store.state.holderState.credentials
                    val credentialsToDelete = store.state.holderState.credentialsToDelete
                    if (credentialsOnCard != credentialsWithChanges) {
                        val filesToChangeVisibility = store.state.holderState.getFilesToChangeVisibility()
                        tangemIdSdk.holder.changeHoldersCredentials(
                            store.state.holderState.cardId, credentialsToDelete, filesToChangeVisibility
                        ) { result ->
                            mainThread.post {
                                when (result) {
                                    SimpleResponse.Success -> store.dispatch(HolderAction.SaveChanges.Success)
                                    is SimpleResponse.Failure ->
                                        store.dispatch(HolderAction.SaveChanges.Failure(result.error))
                                }
                            }
                        }
                    }
                }
                is HolderAction.ChangePasscodeAction -> {
                    tangemIdSdk.holder.changePasscode(store.state.holderState.cardId) { result ->
                        mainThread.post {
                            when (result) {
                                SimpleResponse.Success ->
                                    store.dispatch(HolderAction.ChangePasscodeAction.Success)
                                is SimpleResponse.Failure ->
                                    store.dispatch(HolderAction.ChangePasscodeAction.Failure(result.error))
                            }
                        }
                    }
                }
                is HolderAction.RequestNewCredential -> {
                    tangemIdSdk.holder.addCovidCredential { result ->
                        mainThread.post {
                            when (result) {
                                is CompletionResult.Success -> {
                                    val holdersCredentials =
                                        result.data.map { it.toHolderCredential() }
                                    store.dispatch(
                                        HolderAction.RequestNewCredential.Success(holdersCredentials)
                                    )
                                }
                                is CompletionResult.Failure ->
                                    store.dispatch(HolderAction.RequestNewCredential.Failure(result.error))
                            }
                        }
                    }
                }
            }
            next(action)
        }
    }
}