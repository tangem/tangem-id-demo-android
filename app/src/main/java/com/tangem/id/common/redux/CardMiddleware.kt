package com.tangem.id.common.redux

import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.common.extensions.toByteArray
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.features.home.HomeAction
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.features.issuer.redux.IssuerAction
import com.tangem.id.store
import com.tangem.id.tangemIdSdk
import org.rekotlin.Middleware

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
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.Verifier))

                        }
                    }
                }
                is HomeAction.ReadCredentialsAsHolder -> {
                    tangemIdSdk.readCredentialsAsHolder { credentials ->
                        if (credentials != null) {
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.Holder))

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
            }
            next(action)
        }
    }
}