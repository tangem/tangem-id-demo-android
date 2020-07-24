package com.tangem.id.features.issuecredentials.ui.widgets

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.extensions.setMargins
import com.tangem.id.common.redux.AgeOfMajority
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.layout_checkbox_card.*

class EditableAgeOfMajorityWidget(private val fragment: Fragment) :
    CredentialWidget<AgeOfMajority>(fragment.context) {
    override val viewToInflate = R.layout.layout_checkbox_card
    override val rootView: ViewGroup? = fragment.fl_checkbox

    override fun inflate(parent: ViewGroup) {
        super.inflate(parent)
        fragment.card_checkbox?.setMargins()
        fragment.tv_checkbox_title.text = fragment.getString(R.string.credential_age_of_majority)
    }

    override fun setup(credential: AgeOfMajority, editable: Boolean) {
        credential.valid?.let { fragment.checkbox?.isChecked = it }
        fragment.checkbox?.setOnCheckedChangeListener { _, isChecked ->
            store.dispatch(IssueCredentialsAction.SaveAgeOfMajority(isChecked))
        }
    }
}