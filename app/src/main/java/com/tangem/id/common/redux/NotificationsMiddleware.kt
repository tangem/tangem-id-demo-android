package com.tangem.id.common.redux

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.tangem.TangemError
import com.tangem.id.notificationsHandler
import org.rekotlin.Action
import org.rekotlin.Middleware
import java.lang.ref.WeakReference

class NotificationsHandler(coordinatorLayout: CoordinatorLayout) {
    private val coordinatorLayoutWeak = WeakReference(coordinatorLayout)

    fun showNotification(message: String) {
        coordinatorLayoutWeak.get()?.let { layout ->
            Snackbar.make(layout, message, Snackbar.LENGTH_LONG)
                .also { snackbar -> snackbar.show() }
        }
    }

    fun showNotification(message: Int) {
        coordinatorLayoutWeak.get()?.let {
            showNotification(it.context.getString(message))
        }
    }
}

val notificationsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            if (action is NotificationAction) {
//                if (action.message != null) {
//                    action.message?.let { notificationsHandler.showNotification(it) }
//                } else {
//                    action.messageResource?.let {
                notificationsHandler?.showNotification(action.messageResource)
//                    }
//                }
            }
            if (action is ErrorAction) {
                notificationsHandler?.showNotification(action.error.customMessage)
            }
            next(action)
        }
    }
}

interface NotificationAction : Action {
    val messageResource: Int
}

interface ErrorAction : Action {
    val error: TangemError
}