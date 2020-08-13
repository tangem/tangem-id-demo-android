package com.tangem.id.features.issuecredentials.ui.widgets

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.tangem.id.R
import com.tangem.id.common.entities.Gender
import com.tangem.id.common.entities.Passport
import com.tangem.id.common.extensions.toDate
import com.tangem.id.common.extensions.toMillis
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.features.issuecredentials.ui.textwatchers.DateFormattingTextWatcher
import com.tangem.id.store
import kotlinx.android.synthetic.main.layout_passport_editable.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class EditablePersonalInfoWidget(private val fragment: Fragment) :
    CredentialWidget<Passport>(fragment.context) {
    override val viewToInflate = R.layout.layout_passport_editable
    override val rootView: ViewGroup? = fragment.fl_passport_editable

    override fun setup(credential: Passport, editable: Boolean) {

        credential.name?.let { fragment.et_name?.setText(it) }
        credential.surname?.let { fragment.et_surname?.setText(it) }
        credential.gender?.let { fragment.radio_group_gender?.check(it.toRadioButtonId(fragment)) }
        credential.birthDate?.let { fragment.et_date?.setText(it) }

        if (editable) {
            fragment.et_name.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    onEditEnded(view)
                }
            }
            fragment.et_surname.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    onEditEnded(view)
                }
            }
            fragment.radio_group_gender?.setOnCheckedChangeListener { radioGroup, checkedId ->
                onEditEnded(radioGroup)
            }
            fragment.et_date.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    onEditEnded(view)
                }
            }

            fragment.iv_date_picker.setOnClickListener {
                onEditEnded(fragment.iv_date_picker)
                launchDatePicker()
            }

            fragment.et_date.addTextChangedListener(DateFormattingTextWatcher())

            if (credential.isDateValid() == false) {
                fragment.til_date.error = "Date format is MM/dd/yyyy"
            } else {
                fragment.til_date.error = null
            }

        }

        fragment.et_name.isEnabled = editable
        fragment.et_surname.isEnabled = editable
        fragment.radio_group_gender.isEnabled = editable
        fragment.radio_gender_female.isEnabled = editable
        fragment.radio_gender_male.isEnabled = editable
        fragment.radio_gender_other.isEnabled = editable
        fragment.et_date.isEnabled = editable
    }

    private fun onEditEnded(view: View) {
//        view.hideKeyboard()

        val name = fragment.et_name?.text?.toString()
        val surname = fragment.et_surname?.text?.toString()
        val gender = radioButtonIdToGender(
            fragment.radio_group_gender?.checkedRadioButtonId, fragment
        )
        val date = fragment.et_date?.text?.toString()
        store.dispatch(IssueCredentialsAction.SaveInput(Passport(name, surname, gender, date)))
    }

    private fun Gender.toRadioButtonId(fragment: Fragment): Int {
        return when (this) {
            Gender.Male -> fragment.radio_gender_male.id
            Gender.Female -> fragment.radio_gender_female.id
            Gender.Other -> fragment.radio_gender_other.id
        }
    }

    private fun radioButtonIdToGender(id: Int?, fragment: Fragment): Gender? {
        return when (id) {
            fragment.radio_gender_male.id -> Gender.Male
            fragment.radio_gender_female.id -> Gender.Female
            fragment.radio_gender_other.id -> Gender.Other
            else -> null
        }
    }

    private fun launchDatePicker() {
        val selectedDate = fragment.et_date?.text?.toString()?.toDate()?.toMillis()

        val builder = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(limitRange(selectedDate))
        if (selectedDate != null) builder.setSelection(selectedDate)
        val picker = builder.build()
        picker.show(fragment.childFragmentManager, picker.toString())
        picker.addOnPositiveButtonClickListener { time ->
            val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
            val dateString = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            store.dispatch(IssueCredentialsAction.SaveInput(Passport(null, null, null, dateString)))
        }
    }

    private fun limitRange(selectAt: Long?): CalendarConstraints {

        val constraintsBuilderRange = CalendarConstraints.Builder()

        val calendarEnd: Calendar = GregorianCalendar.getInstance()
        val calendarStart: Calendar = GregorianCalendar.getInstance()

        val startYear = calendarEnd.get(Calendar.YEAR) - 150
        calendarStart.set(startYear, 10, 17)

        val minDate = calendarStart.timeInMillis
        val maxDate = calendarEnd.timeInMillis

        constraintsBuilderRange.setStart(minDate)
        constraintsBuilderRange.setEnd(maxDate)

        if (selectAt != null && minDate <= selectAt && maxDate >= selectAt) {
            constraintsBuilderRange.setOpenAt(selectAt)
        }

        return constraintsBuilderRange.build()
    }
}