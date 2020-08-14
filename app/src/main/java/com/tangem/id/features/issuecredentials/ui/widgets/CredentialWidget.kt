package com.tangem.id.features.issuecredentials.ui.widgets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.id.common.entities.*

abstract class CredentialWidget<in T : Credential>(private val context: Context?) {
    protected abstract val viewToInflate: Int
    protected abstract val rootView: ViewGroup?
    private var inflatedView: View? = null

    open fun inflate(parent: ViewGroup) {
        if (rootView == null) {
            inflatedView = LayoutInflater.from(context).inflate(viewToInflate, rootView)
            parent.addView(inflatedView)
        }
    }

    abstract fun setup(credential: T, editable: Boolean = true)

    fun inflateAndSetup(credential: T, parent: ViewGroup, editable: Boolean = true) {
        inflate(parent)
        setup(credential, editable)
    }
}

class CredentialWidgetFactory(private val fragment: Fragment) {
    fun <T : Credential> createFrom(credential: T): CredentialWidget<T>? {
        val widget = when (credential) {
            is Photo -> EditablePhotoWidget(fragment)
            is Passport -> EditablePersonalInfoWidget(fragment)
            is SecurityNumber -> EditableSsnWidget(fragment)
            is AgeOfMajority -> EditableAgeOfMajorityWidget(fragment)
            else -> null
        }
        return widget as? CredentialWidget<T>
    }
}