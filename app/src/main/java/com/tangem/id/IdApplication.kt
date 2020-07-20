package com.tangem.id

import android.app.Application
import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.appReducer
import com.tangem.id.common.redux.navigation.navigationMiddleware
import org.rekotlin.Store


val store = Store(
    reducer = ::appReducer,
    middleware = listOf(navigationMiddleware),
    state = AppState()
)

class IdApplication : Application()