package com.tangem.id

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.id.common.redux.navigation.NavigationAction

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (supportFragmentManager.backStackEntryCount == 0) {
            store.dispatch(
                NavigationAction.RestoreSavedBackStack(store.state.navigationState.backStack, this)
            )
        }
    }
}