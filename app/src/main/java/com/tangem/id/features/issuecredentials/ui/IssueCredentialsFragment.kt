package com.tangem.id.features.issuecredentials.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.tangem.id.R
import com.tangem.id.common.extensions.hide
import com.tangem.id.common.extensions.hideKeyboard
import com.tangem.id.common.extensions.shareText
import com.tangem.id.common.extensions.show
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsButton
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsState
import com.tangem.id.features.issuecredentials.ui.widgets.CredentialWidgetFactory
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_issue_credentials.*
import kotlinx.android.synthetic.main.layout_button.*
import kotlinx.android.synthetic.main.layout_show_json_button.*
import org.rekotlin.StoreSubscriber


class IssueCredentialsFragment : Fragment(R.layout.fragment_issue_credentials),
    StoreSubscriber<IssueCredentialsState> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleExit()
            }
        })
    }

    private fun handleExit() {
        if (store.state.issueCredentialsState == IssueCredentialsState()
            || !store.state.issueCredentialsState.editable
        ) {
            store.dispatch(IssueCredentialsAction.ResetState)
            store.dispatch(NavigationAction.PopBackTo())
        } else {
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        val builder = MaterialAlertDialogBuilder(context)
        builder
            .setMessage(R.string.issue_credentials_dialog_go_back)
            .setPositiveButton(com.tangem.tangem_sdk_new.R.string.general_ok)
            { _, _ ->
                store.dispatch(IssueCredentialsAction.ResetState)
                store.dispatch(NavigationAction.PopBackTo())
            }
            .setNegativeButton(com.tangem.tangem_sdk_new.R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.issueCredentialsState == newState.issueCredentialsState
            }.select { it.issueCredentialsState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener { handleExit() }
        setupUI(ll_root)
    }

    private fun getFocusedView(viewGroup: ViewGroup): View? {
        val focusedChild = viewGroup.focusedChild
        return if (focusedChild == null || focusedChild !is ViewGroup) {
            focusedChild
        } else {
            getFocusedView(focusedChild)
        }
    }

    private fun setupUI(view: View) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { v, event ->
                view.hideKeyboard()
                return@setOnTouchListener false
            }
        }

        //If a layout container, iterate over children and seed recursion.
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    override fun newState(state: IssueCredentialsState) {
        if (activity == null) return

        val focusedView = getFocusedView(ll_root)
        if (focusedView !is TextInputEditText
        ) {
            ll_root.hideKeyboard()
        }



        if (state.issueCredentialsCompleted) {
//            store.dispatch(IssueCredentialsAction.ResetState)
//            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
//            return
        }


        val credentialWidgetFactory =
            CredentialWidgetFactory(
                this
            )
        state.getCredentials().map { credential ->
            credentialWidgetFactory.createFrom(credential)
                ?.inflateAndSetup(credential, ll_root, state.editable)
        }

        if (fl_button == null) {
            val buttonView = LayoutInflater.from(context).inflate(R.layout.layout_button, fl_button)
            ll_root.addView(buttonView)
        }
        when (state.button) {
            is IssueCredentialsButton.Sign -> {
                progress_btn?.hide()
                btn_filled?.text = getString(R.string.issue_credentials_btn_sign)
                if (state.isInputDataReady()) {
                    btn_filled?.isEnabled = true
                    btn_filled?.setOnClickListener {
                        store.dispatch(IssueCredentialsAction.Sign(requireContext().applicationContext))
                    }
                } else {
                    btn_filled?.isEnabled = false
                }
                if (state.button.progress) {
                    btn_filled?.text = null
                    progress_btn?.show()
                    return
                }
            }
            is IssueCredentialsButton.WriteCredentials -> {
                progress_btn?.hide()
                btn_filled.isEnabled = true
                btn_filled?.text = getString(R.string.issue_credentials_btn_write)
                btn_filled?.setOnClickListener {
                    store.dispatch(IssueCredentialsAction.WriteCredentials)
                }
            }
        }
        if (!state.editable && fl_button_json == null) {
            val buttonView = LayoutInflater.from(context).inflate(
                R.layout.layout_show_json_button, fl_button_json
            )
            ll_root.addView(buttonView)
            btn_show_json.setOnClickListener { store.dispatch(IssueCredentialsAction.ShowJson) }
        }
        if (!state.editable && state.jsonShown != null) {
            val builder = MaterialAlertDialogBuilder(context)
            builder
                .setMessage(state.jsonShown)
                .setOnDismissListener { store.dispatch(IssueCredentialsAction.HideJson) }
                .setPositiveButton(getText(R.string.credential_dialog_share))
                { _, _ -> context?.shareText(state.jsonShown) }
                .setNegativeButton(getText(R.string.credential_dialog_hide)) { dialog, _ -> dialog.cancel() }
            val dialog = builder.create()
            dialog.show()
        }
    }
}



