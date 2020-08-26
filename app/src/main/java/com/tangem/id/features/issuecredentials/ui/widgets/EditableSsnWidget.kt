package com.tangem.id.features.issuecredentials.ui.widgets

import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.entities.SecurityNumber
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.features.issuecredentials.ui.textwatchers.SsnFormattingTextWatcher
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
                val number = fragment.et_ssn.text.toString()
                store.dispatch(IssueCredentialsAction.SaveInput(ssn = number))
            }
        }
        fragment.et_ssn.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER) {
                fragment.et_ssn.clearFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false;
        }
        fragment.et_ssn.isEnabled = editable
        fragment.et_ssn.addTextChangedListener(SsnFormattingTextWatcher())

        if (!credential.number.isNullOrBlank() && !credential.isDataPresent()) {
            fragment.et_ssn.error = fragment.getString(R.string.issue_credentials_ssn_error)
        } else {
            fragment.et_ssn.error = null
        }
    }
}
