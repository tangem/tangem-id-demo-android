package com.tangem.id

import android.app.Application
import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.appReducer
import org.rekotlin.Store


val store = Store(
    reducer = ::appReducer,
    middleware = AppState.getMiddleware(),
    state = AppState()
)

class IdApplication : Application()