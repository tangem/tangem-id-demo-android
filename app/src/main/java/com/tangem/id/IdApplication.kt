package com.tangem.id

import android.app.Application
import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.appReducer
import com.tangem.id.common.redux.cardMiddleware
import com.tangem.id.common.redux.navigation.navigationMiddleware
import com.tangem.id.common.redux.notificationsMiddleware
import org.rekotlin.Store


val store = Store(
    reducer = ::appReducer,
    middleware = listOf(navigationMiddleware, notificationsMiddleware, cardMiddleware),
    state = AppState()
)

class IdApplication : Application()