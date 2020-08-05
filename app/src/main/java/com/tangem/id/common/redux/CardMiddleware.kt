package com.tangem.id.common.redux

import android.os.Handler
import android.os.Looper
import com.tangem.common.CompletionResult
import com.tangem.id.SimpleResponse
import com.tangem.id.common.extensions.toByteArray
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.features.holder.redux.AccessLevel
import com.tangem.id.features.holder.redux.HolderAction
import com.tangem.id.features.home.HomeAction
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.features.issuer.redux.IssuerAction
import com.tangem.id.features.verifier.redux.VerifierAction
import com.tangem.id.features.verifier.redux.VerifierCredential
import com.tangem.id.store
import com.tangem.id.tangemIdSdk
import org.rekotlin.Middleware

private val mainThread = Handler(Looper.getMainLooper())

val cardMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is HomeAction.ReadIssuerCard -> {
                    tangemIdSdk.readIssuerCard { result ->
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
                    tangemIdSdk.readCredentialsAsVerifier { result ->
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
                    tangemIdSdk.readCredentialsAsHolder { result ->
                        mainThread.post {
                            when (result) {
                                is CompletionResult.Failure ->
                                    store.dispatch(HomeAction.ReadCredentialsAsHolder.Failure(result.error))
                                is CompletionResult.Success -> {
                                    val holdersCredentials =
                                        result.data.credentials.map {
                                            Credential.from(it.first.decodedCredential)
                                        }
                                    val visibility =
                                        result.data.credentials.map { AccessLevel.from(it.second) }

                                    store.dispatch(
                                        HolderAction.CredentialsRead(
                                            result.data.cardId, holdersCredentials.zip(visibility)
                                        )
                                    )
                                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Holder))
                                }

                            }
                        }

                    }
                }
                is IssuerAction.ReadHoldersCard -> {
                    tangemIdSdk.getHolderAddress { result ->
                        mainThread.post {
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
                is IssueCredentialsAction.Sign -> {
                    val credentialsState = store.state.issueCredentialsState
                    val data = DemoPersonData(
                        credentialsState.passport!!.name!!,
                        credentialsState.passport.surname!!,
                        credentialsState.passport.gender.toString(),
                        credentialsState.passport.birthDate!!,
                        credentialsState.securityNumber!!.number!!,
                        credentialsState.photo!!.photo!!.toByteArray()
                    )
                    tangemIdSdk.formCredentialsAndSign(
                        data, store.state.issueCredentialsState.holdersAddress!!
                    ) { result ->
                        mainThread.post {
                            when (result) {
                                SimpleResponse.Success -> store.dispatch(IssueCredentialsAction.Sign.Success)
                                is SimpleResponse.Failure ->
                                    store.dispatch(IssueCredentialsAction.Sign.Failure(result.error))
                            }
                        }
                    }
                }
                is IssueCredentialsAction.WriteCredentials -> {
                    tangemIdSdk.writeCredentialsAndSend { result ->
                        mainThread.post {
                            when (result) {
                                SimpleResponse.Success -> {
                                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                                    store.dispatch(
                                        IssueCredentialsAction.WriteCredentials.Success()
                                    )
                                }
                                is SimpleResponse.Failure -> store.dispatch(
                                    IssueCredentialsAction.WriteCredentials.Failure(result.error)
                                )
                            }
                        }
                    }
                }
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
                        tangemIdSdk.changeHoldersCredentials(
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
                    tangemIdSdk.changePasscode { result ->
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
                    tangemIdSdk.addCovidCredential { result ->
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
//                                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Holder))
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