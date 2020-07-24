package com.tangem.id.common.redux.navigation

import androidx.fragment.app.FragmentActivity
import org.rekotlin.Action
import java.lang.ref.WeakReference

sealed class NavigationAction : Action {
    data class NavigateTo(
        val screen: AppScreen,
        val activity: WeakReference<FragmentActivity>,
        val addToBackstack: Boolean = true
    ) : NavigationAction() {

        constructor(screen: AppScreen, activity: FragmentActivity, addToBackstack: Boolean = true) :
                this(screen, WeakReference(activity), addToBackstack)
    }

    data class PopBackTo(
        val screen: AppScreen?,
        val activity: WeakReference<FragmentActivity>
    ) : NavigationAction() {

        constructor(screen: AppScreen? = null, activity: FragmentActivity) :
                this(screen, WeakReference(activity))
    }

    data class RestoreSavedBackStack(
        val backStack: List<AppScreen>,
        val activity: WeakReference<FragmentActivity>
    ) :
        NavigationAction() {
        constructor(backStack: List<AppScreen>, activity: FragmentActivity) :
                this(backStack, WeakReference(activity))
    }
}