package com.tangem.id.common.redux

import android.os.Handler
import android.os.Looper
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
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
                    tangemIdSdk.readIssuerCard { address ->
                        if (address != null) {
                            store.dispatch(IssuerAction.AddAddress(address))
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.Issuer))
                        } else {
                            //TODO: handle failure
                        }
                    }
                }
                is HomeAction.ReadCredentialsAsVerifier -> {
                    tangemIdSdk.readCredentialsAsVerifier { credentials ->
                        if (credentials != null) {
                            val verifierCredentials =
                                credentials.mapNotNull { VerifierCredential.from(it) }
                            store.dispatch(VerifierAction.CredentialsRead(verifierCredentials))
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.Verifier))
                        }
                    }
                }
                is HomeAction.ReadCredentialsAsHolder -> {
                    tangemIdSdk.readCredentialsAsHolder { result ->
                        when (result) {
                            is Result.Failure -> store.dispatch(HomeAction.ReadCredentialsAsHolder.Failure)
                            is Result.Success -> {
                                val holdersCredentials =
                                    result.data.credentials.map { Credential.from(it.first.decodedCredential) }
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
                is IssuerAction.ReadHoldersCard -> {
                    tangemIdSdk.getHolderAddress { address ->
                        if (address != null) {
                            store.dispatch(IssueCredentialsAction.AddHoldersAddress(address))
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.IssueCredentials))
                        } else {
                            //TODO: handle failure
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
                        when (result) {
                            SimpleResult.Success -> store.dispatch(IssueCredentialsAction.Sign.Success)
                            is SimpleResult.Failure -> store.dispatch(IssueCredentialsAction.Sign.Failure)
                        }
                    }
                }
                is IssueCredentialsAction.WriteCredentials -> {
                    tangemIdSdk.writeCredentialsAndSend { result ->
                        when (result) {
                            SimpleResult.Success -> store.dispatch(IssueCredentialsAction.WriteCredentials.Success)
                            is SimpleResult.Failure -> store.dispatch(IssueCredentialsAction.WriteCredentials.Failure)
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
                                    SimpleResult.Success -> store.dispatch(HolderAction.SaveChanges.Success)
                                    is SimpleResult.Failure -> store.dispatch(HolderAction.SaveChanges.Failure)
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