package com.tangem.id.common.redux.navigation

import org.rekotlin.StateType

data class NavigationState(val backStack: List<AppScreen> = listOf(AppScreen.Home)) : StateType

enum class AppScreen { Home, Verifier, Holder, Issuer, IssueCredential, Camera, QrScan }
