package com.tangem.id.features.holder.redux

import android.os.Handler
import android.os.Looper
import com.tangem.common.CompletionResult
import com.tangem.id.SimpleResponse
import com.tangem.id.common.entities.Credential
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
                        val indicesToDelete: List<Int> = if (credentialsToDelete.isNotEmpty()) {
                            val originalCredentials = credentialsOnCard.unzip().first
                            credentialsToDelete.map { originalCredentials.indexOf(it) }
                        } else {
                            listOf()
                        }
                        val indicesWithNewVisibility = credentialsOnCard.mapIndexed { index, pair ->
                            if (credentialsWithChanges.find { it.first == pair.first && it.second != pair.second } != null) {
                                index
                            } else null
                        }.filterNotNull()
                        tangemIdSdk.holder.changeHoldersCredentials(
                            store.state.holderState.cardId,
                            indicesToDelete, indicesWithNewVisibility
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
                                        result.data.map {
                                            Credential.from(it.first.decodedCredential) to AccessLevel.from(
                                                it.second
                                            )
                                        }
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