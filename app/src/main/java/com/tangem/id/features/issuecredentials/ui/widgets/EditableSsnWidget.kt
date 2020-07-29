package com.tangem.id.features.issuecredentials.ui.widgets

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.extensions.hideKeyboard
import com.tangem.id.common.redux.SecurityNumber
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.features.issuecredentials.ui.textwatchers.SsnTextWatcher
import com.tangem.id.store
import kotlinx.android.synthetic.main.layout_ssn_editable.*


class EditableSsnWidget(private val fragment: Fragment) :
    CredentialWidget<SecurityNumber>(fragment.context) {
    override val viewToInflate = R.layout.layout_ssn_editable
    override val rootView: ViewGroup? = fragment.fl_ssn_editable

    override fun setup(credential: SecurityNumber, editable: Boolean) {
        credential.number?.let { fragment.et_ssn?.setText(it) }
        fragment.et_ssn.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                view.hideKeyboard()
                val number = fragment.et_ssn.text.toString()
                store.dispatch(IssueCredentialsAction.SaveSecurityNumber(number))
            }
        }
        fragment.et_ssn.isEnabled = editable
        fragment.et_ssn.addTextChangedListener(SsnTextWatcher())
//        val listener = MaskedTextChangedListener("[000]-[00]-[000]", fragment.et_ssn)

//        fragment.et_ssn.addTextChangedListener(listener)
    }
}
