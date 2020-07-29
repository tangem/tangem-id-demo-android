package com.tangem.id.common.redux.navigation

import com.tangem.id.common.extensions.openFragment
import com.tangem.id.common.extensions.popBackTo
import com.tangem.id.common.extensions.restoreBackStack
import com.tangem.id.common.redux.AppState
import com.tangem.id.store
import org.rekotlin.Middleware

val navigationMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            if (action is NavigationAction) {
                val navState = store.state.navigationState
                when (action) {
                    is NavigationAction.NavigateTo -> {
                        navState.activity?.get()?.openFragment(action.screen, action.addToBackstack)
                    }
                    is NavigationAction.PopBackTo -> navState.activity?.get()
                        ?.popBackTo(action.screen)
                    is NavigationAction.RestoreSavedBackStack -> {
                        navState.activity?.get()?.restoreBackStack(action.backStack)
                    }
                }
            }
            next(action)
        }
    }
}