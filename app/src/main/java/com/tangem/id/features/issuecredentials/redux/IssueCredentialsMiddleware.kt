package com.tangem.id.features.issuecredentials.redux

import android.os.Handler
import android.os.Looper
import com.tangem.id.SimpleResponse
import com.tangem.id.TangemIdError
import com.tangem.id.common.entities.Passport
import com.tangem.id.common.entities.Photo
import com.tangem.id.common.entities.SecurityNumber
import com.tangem.id.common.extensions.toByteArray
import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.store
import com.tangem.id.tangemIdSdk
import org.rekotlin.Middleware

private val mainThread = Handler(Looper.getMainLooper())

val issueCredentialsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is IssueCredentialsAction.Sign -> {
                    val credentialsState = store.state.issueCredentialsState
                    val data = createDemoPersonData(
                        credentialsState.passport, credentialsState.photo,
                        credentialsState.securityNumber
                    )
                    tangemIdSdk.issuer.formCredentialsAndSign(
                        data!!, store.state.issueCredentialsState.holdersAddress!!
                    ) { result ->
                        mainThread.post {
                            when (result) {
                                SimpleResponse.Success -> store.dispatch(IssueCredentialsAction.Sign.Success)
                                is SimpleResponse.Failure ->
                                    if (result.error is TangemIdError.UserCancelled) {
                                        store.dispatch(IssueCredentialsAction.Sign.Cancelled)
                                    } else {
                                        store.dispatch(
                                            IssueCredentialsAction.Sign.Failure(result.error)
                                        )
                                    }
                            }
                        }
                    }
                }
                is IssueCredentialsAction.WriteCredentials -> {
                    tangemIdSdk.issuer.writeCredentialsAndSend { result ->
                        mainThread.post {
                            when (result) {
                                SimpleResponse.Success -> {
                                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                                    store.dispatch(
                                        IssueCredentialsAction.WriteCredentials.Success()
                                    )
                                }
                                is SimpleResponse.Failure -> if (result.error is TangemIdError.UserCancelled) {
                                    store.dispatch(IssueCredentialsAction.WriteCredentials.Cancelled)
                                } else {
                                    store.dispatch(
                                        IssueCredentialsAction.WriteCredentials.Failure(result.error)
                                    )
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

private fun createDemoPersonData(
    passport: Passport?,
    photo: Photo?,
    ssn: SecurityNumber?
): DemoPersonData? {
    val date = passport?.dateFormatted()
    val securityNumber = ssn?.ssnFormatted()
    return if (passport?.name != null && passport.surname != null && passport.gender != null
        && date != null && photo?.photo != null && securityNumber != null
    ) {
        DemoPersonData(
            passport.name, passport.surname, passport.gender.toString(), date, securityNumber,
            photo.photo.toByteArray()
        )
    } else {
        null
    }
}