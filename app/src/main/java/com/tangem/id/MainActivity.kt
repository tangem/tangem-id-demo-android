package com.tangem.id

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.tangem.id.common.redux.NotificationsHandler
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

lateinit var tangemIdSdk: TangemIdSdk
var notificationsHandler: NotificationsHandler? = null

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

    override fun onResume() {
        super.onResume()
        notificationsHandler = NotificationsHandler(fragment_container)
    }

    override fun onStop() {
        notificationsHandler = null
        super.onStop()
    }

    override fun onDestroy() {
        store.dispatch(NavigationAction.ActivityDestroyed)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.change_passcode_menu -> {
                return false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}