package com.tangem.id.common.extensions

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.card.MaterialCardView
import com.tangem.id.R
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.features.holder.ui.HolderFragment
import com.tangem.id.features.holder.ui.QrScanFragment
import com.tangem.id.features.home.HomeFragment
import com.tangem.id.features.issuecredentials.ui.CameraFragment
import com.tangem.id.features.issuecredentials.ui.IssueCredentialsFragment
import com.tangem.id.features.issuer.IssuerFragment
import com.tangem.id.features.verifier.VerifierFragment

fun Fragment.getDrawable(@DrawableRes drawableResId: Int): Drawable? {
    return ContextCompat.getDrawable(requireContext(), drawableResId)
}

fun View.show(show: Boolean) {
    if (show) this.visibility = View.VISIBLE else this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.makeInvisible() {
    this.visibility = View.INVISIBLE
}

fun Context.dpToPixels(dp: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), this.resources.displayMetrics
    ).toInt()

fun MaterialCardView.setMargins() {
    val params = this.layoutParams
    val margin = context?.dpToPixels(16) ?: 16
    (params as ViewGroup.MarginLayoutParams).setMargins(margin, margin, margin, margin)
    this.layoutParams = params
}

fun Activity.setSystemBarTextColor(setTextDark: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val flags = this.window.decorView.systemUiVisibility
        // Update the SystemUiVisibility dependening on whether we want a Light or Dark theme.
        this.window.decorView.systemUiVisibility =
            if (setTextDark) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
    }
}

fun FragmentActivity.openFragment(screen: AppScreen, addToBackStack: Boolean = true) {
    val transaction = this.supportFragmentManager.beginTransaction()
        .replace(
            R.id.fragment_container,
            fragmentFactory(screen),
            screen.name
        )
    if (addToBackStack && screen != AppScreen.Home) transaction.addToBackStack(null)
    transaction.commit();
}

fun FragmentActivity.popBackTo(screen: AppScreen?) {
    this.supportFragmentManager.popBackStack(screen?.name, 0)
}

fun FragmentActivity.getPreviousScreen() : AppScreen? {
    val indexOfLastFragment = this.supportFragmentManager.backStackEntryCount - 1
    val tag =  this.supportFragmentManager.getBackStackEntryAt(indexOfLastFragment).name
    return tag?.let { AppScreen.valueOf(tag) }
}

fun FragmentActivity.restoreBackStack(backStack: List<AppScreen>) {
    backStack.map { openFragment(it) }
}

private fun fragmentFactory(screen: AppScreen) : Fragment {
    return when (screen) {
        AppScreen.Home -> HomeFragment()
        AppScreen.Verifier -> VerifierFragment()
        AppScreen.Holder -> HolderFragment()
        AppScreen.Issuer -> IssuerFragment()
        AppScreen.IssueCredential -> IssueCredentialsFragment()
        AppScreen.Camera -> CameraFragment()
        AppScreen.QrScan -> QrScanFragment()
    }
}