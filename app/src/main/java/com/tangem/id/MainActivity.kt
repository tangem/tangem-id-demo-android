package com.tangem.id

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import java.lang.ref.WeakReference

lateinit var tangemIdSdk: TangemIdSdk

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        store.dispatch(NavigationAction.ActivityCreated(WeakReference(this)))

        tangemIdSdk = TangemIdSdk(this)

        if (supportFragmentManager.backStackEntryCount == 0) {
            store.dispatch(
                NavigationAction.NavigateTo(AppScreen.Home)
            )
        }
    }

    override fun onDestroy() {
        store.dispatch(NavigationAction.ActivityDestroyed)
        super.onDestroy()
    }
}