package com.tangem.id.features.issuecredentials.ui.widgets

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.redux.SecurityNumber
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.layout_ssn_editable.*

class EditableSsnWidget(private val fragment: Fragment) :
    CredentialWidget<SecurityNumber>(fragment.context) {
    override val viewToInflate = R.layout.layout_ssn_editable
    override val rootView: ViewGroup? = fragment.fl_ssn_editable

    override fun setup(credential: SecurityNumber, editable: Boolean) {
        credential.number?.let { fragment.et_ssn?.setText(it) }
        fragment.et_ssn.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val number = fragment.et_ssn.text.toString()
                store.dispatch(IssueCredentialsAction.SaveSecurityNumber(number))
            }
        }
        fragment.et_ssn.isEnabled = editable
    }
}
