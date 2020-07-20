package com.tangem.id.features.issuecredentials.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsState
import com.tangem.id.features.issuecredentials.redux.NewCredentialsButton
import com.tangem.id.features.issuecredentials.ui.widgets.CredentialWidgetFactory
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_issue_credentials.*
import kotlinx.android.synthetic.main.layout_button.*
import org.rekotlin.StoreSubscriber

class IssueCredentialsFragment : Fragment(R.layout.fragment_issue_credentials),
    StoreSubscriber<IssueCredentialsState> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo(activity = requireActivity()))
            }
        })
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
        toolbar.setNavigationOnClickListener{
            store.dispatch(NavigationAction.PopBackTo(activity = requireActivity()))
        }
    }

    override fun newState(state: IssueCredentialsState) {
        if (activity == null) return

        val credentialWidgetFactory =
            CredentialWidgetFactory(
                this
            )
        state.credentials.map { credential ->
            credentialWidgetFactory.createFrom(credential)
                ?.inflateAndSetup(credential, ll_root, state.editable)
        }

        if (fl_button == null) {
            val buttonView = LayoutInflater.from(context).inflate(R.layout.layout_button, fl_button)
            ll_root.addView(buttonView)
        }
        when (state.button) {
            is NewCredentialsButton.Sign -> {
                btn_filled?.text = getString(R.string.issue_credentials_btn_sign)
                if (state.inputDataReady) {
                    btn_filled?.isEnabled = true
                    btn_filled?.setOnClickListener { store.dispatch(IssueCredentialsAction.Sign) }
                } else {
                    btn_filled?.isEnabled = false
                }
            }
            is NewCredentialsButton.WriteCredentials -> {
                btn_filled.isEnabled = true
                btn_filled?.text = getString(R.string.issue_credentials_btn_write)
                btn_filled?.setOnClickListener {
                    store.dispatch(IssueCredentialsAction.WriteCredentials)
                }
            }
        }
    }
}



