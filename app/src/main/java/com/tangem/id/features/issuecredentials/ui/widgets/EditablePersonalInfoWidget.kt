package com.tangem.id.features.issuecredentials.ui.widgets

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.redux.Gender
import com.tangem.id.common.redux.Passport
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.layout_passport_editable.*

class EditablePersonalInfoWidget(private val fragment: Fragment) :
    CredentialWidget<Passport>(fragment.context) {
    override val viewToInflate = R.layout.layout_passport_editable
    override val rootView: ViewGroup? = fragment.fl_passport_editable

    override fun setup(credential: Passport, editable: Boolean) {
        credential.name?.let { fragment.et_name?.setText(it) }
        credential.surname?.let { fragment.et_surname?.setText(it) }
        credential.gender?.let { fragment.radio_group_gender?.check(it.ordinal) }
        credential.birthDate?.let { fragment.et_date?.setText(it) }
        fragment.et_name.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                store.dispatch(IssueCredentialsAction.SaveName(fragment.et_name.text.toString()))
            }
        }
        fragment.et_surname.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                store.dispatch(IssueCredentialsAction.SaveSurname(fragment.et_surname.text.toString()))
            }
        }
        fragment.radio_group_gender?.setOnCheckedChangeListener { radioGroup, _ ->
            val gender = when (radioGroup.id) {
                R.id.radio_gender_male -> Gender.Male
                R.id.radio_gender_female -> Gender.Female
                else -> Gender.Other
            }
            store.dispatch(IssueCredentialsAction.SaveGender(gender))
        }
        fragment.et_date.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                store.dispatch(IssueCredentialsAction.SaveSurname(fragment.et_date.text.toString()))
            }
        }
        fragment.et_name.isEnabled = editable
        fragment.et_surname.isEnabled = editable
        fragment.radio_group_gender.isEnabled = editable
        fragment.et_date.isEnabled = editable
    }
}