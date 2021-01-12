package com.tangem.id.common.redux.navigation

import androidx.fragment.app.FragmentActivity
import org.rekotlin.StateType
import java.lang.ref.WeakReference

data class NavigationState(
    val backStack: List<AppScreen> = listOf(AppScreen.Home),
    val activity: WeakReference<FragmentActivity>? = null
) : StateType

enum class AppScreen {
    Home,
    Verifier,
    Holder,
    Issuer,
    IssueCredentials,
    Camera,
    QrScan,
    RequestCredentials
}
