package com.tangem.id.common.redux.navigation

import com.tangem.id.common.extensions.openFragment
import com.tangem.id.common.extensions.popBackTo
import com.tangem.id.common.extensions.restoreBackStack
import com.tangem.id.common.redux.AppState
import org.rekotlin.Middleware

val navigationMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            if (action is NavigationAction) {
                when (action) {
                    is NavigationAction.NavigateTo -> {
                        action.activity.get()?.openFragment(action.screen, action.addToBackstack)
                    }
                    is NavigationAction.PopBackTo -> action.activity.get()?.popBackTo(action.screen)
                    is NavigationAction.RestoreSavedBackStack -> {
                        action.activity.get()?.restoreBackStack(action.backStack)
                    }
                }
            }
            next(action)
        }
    }
}