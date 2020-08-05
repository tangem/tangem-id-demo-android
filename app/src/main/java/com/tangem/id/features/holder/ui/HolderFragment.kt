package com.tangem.id.features.holder.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tangem.id.R
import com.tangem.id.common.extensions.getDrawable
import com.tangem.id.common.extensions.hide
import com.tangem.id.common.extensions.setSystemBarTextColor
import com.tangem.id.common.extensions.show
import com.tangem.id.common.redux.*
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.holder.redux.HolderAction
import com.tangem.id.features.holder.redux.HolderScreenButton
import com.tangem.id.features.holder.redux.HolderState
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_holder.*
import kotlinx.android.synthetic.main.layout_button.*
import kotlinx.android.synthetic.main.layout_checkbox_card.*
import kotlinx.android.synthetic.main.layout_covid.*
import kotlinx.android.synthetic.main.layout_json_dialog.*
import kotlinx.android.synthetic.main.layout_passport.*
import kotlinx.android.synthetic.main.layout_photo.*
import kotlinx.android.synthetic.main.layout_ssn.*
import org.rekotlin.StoreSubscriber


class HolderFragment : Fragment(R.layout.fragment_holder), StoreSubscriber<HolderState> {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: HolderCredentialsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        setHasOptionsMenu(true)

        requireActivity().setSystemBarTextColor(true)

        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }

        viewManager = LinearLayoutManager(context)
        viewAdapter =
            HolderCredentialsAdapter { action ->
                store.dispatch(action)
            }

        recyclerView = rv_holder_credentials.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.holderState == newState.holderState
            }.select { it.holderState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: HolderState) {
        if (activity == null) return

        viewAdapter.setItems(state.credentials, state.editActivated)

        toggleEditIcon(state.editActivated)

        state.cardId?.let { tv_card_id?.text = it.chunked(4).joinToString(" ") }

        iv_edit_credentials?.setOnClickListener {
            store.dispatch(HolderAction.ToggleEditCredentials)
        }

        if (state.detailsOpened != null) {
            showDetails(state)
        }
        when (state.button) {
            is HolderScreenButton.SaveChanges -> {
                btn_filled?.text = getString(R.string.holder_screen_btn_save)
                btn_filled.setOnClickListener { store.dispatch(HolderAction.SaveChanges) }
            }
            is HolderScreenButton.RequestNewCredential -> {
                btn_filled?.text = getString(R.string.holder_screen_btn_request)
                btn_filled?.setOnClickListener {
//                    store.dispatch(HolderAction.RequestNewCredential)
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.QrScan))
                }
            }
        }
    }

    private fun toggleEditIcon(editActivated: Boolean) {
        val image = if (editActivated) {
            getDrawable(R.drawable.ic_baseline_close_24)
        } else {
            getDrawable(R.drawable.ic_baseline_edit_24)
        }
        iv_edit_credentials?.setImageDrawable(image)
    }

    private fun showDetails(state: HolderState) {
        if (state.jsonShown != null) {
            dialog?.setContentView(R.layout.layout_json_dialog)
            dialog?.tv_json?.text = state.jsonShown
            return
        }
        dialog = Dialog(requireContext())
        dialog?.setOnDismissListener { store.dispatch(HolderAction.HideCredentialDetails) }
        state.detailsOpened?.let { credential -> fillInCredentialDetails(dialog!!, credential) }
        dialog?.show()
    }

    private fun fillInCredentialDetails(dialog: Dialog, credential: Credential) {
        var showJsonButton: Button? = null
        when (credential) {
            is Passport -> {
                dialog.setContentView(R.layout.layout_passport)
                credential.name?.let { dialog.tv_name?.setText(it) }
                credential.surname?.let { dialog.tv_surname?.setText(it) }
                credential.gender?.let { dialog.tv_gender?.setText(it.toString()) }
                credential.birthDate?.let { dialog.tv_birth_date?.setText(it) }
                showJsonButton = dialog.btn_passport_json
                dialog.v_separator_passport?.hide()
                dialog.l_credential_status_passport?.hide()
            }
            is Photo -> {
                dialog.setContentView(R.layout.layout_photo)
                credential.photo?.let { dialog.iv_photo?.setImageBitmap(it) }
                dialog.l_credential_status_photo?.hide()
                showJsonButton = dialog.btn_photo_json

            }
            is SecurityNumber -> {
                dialog.setContentView(R.layout.layout_ssn)
                credential.number?.let { dialog.tv_ssn?.setText(it) }
                dialog.v_separator_ssn?.hide()
                dialog.l_credential_status_ssn?.hide()
                showJsonButton = dialog.btn_ssn_json
            }
            is AgeOfMajority -> {
                dialog.setContentView(R.layout.layout_checkbox_card)
                dialog.checkbox?.isChecked = credential.valid
                dialog.checkbox?.isEnabled = false
                dialog.v_separator_age_of_majority?.hide()
                dialog.l_credential_status_age_of_majority?.hide()
                showJsonButton = dialog.btn_age_of_majority_json
            }
            is ImmunityPassport -> {
                dialog.setContentView(R.layout.layout_covid)
                dialog.checkbox?.isChecked = credential.valid
                dialog.checkbox?.isEnabled = false
                dialog.v_separator_covid?.hide()
                dialog.l_credential_status_covid?.hide()
                showJsonButton = dialog.btn_covid_json
            }
        }
        showJsonButton?.show()
        showJsonButton?.setOnClickListener { store.dispatch(HolderAction.ShowJson(credential)) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.change_passcode_menu -> {
                store.dispatch(HolderAction.ChangePasscodeAction)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.holder, menu)
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val spanString = SpannableString(menu.getItem(i).title.toString())
            spanString.setSpan(
                ForegroundColorSpan(Color.WHITE),
                0,
                spanString.length,
                0
            ) //fix the color to white
            item.setTitle(spanString)
        }
//        super.onCreateOptionsMenu(menu, inflater)
    }

}