package com.tangem.id.features.issuer

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.issuer.redux.IssuerAction
import com.tangem.id.features.issuer.redux.IssuerState
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_issuer.*
import org.rekotlin.StoreSubscriber

class IssuerFragment : Fragment(R.layout.fragment_issuer), StoreSubscriber<IssuerState> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.issuerState == newState.issuerState
            }.select { it.issuerState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: IssuerState) {
        if (activity == null) return

        iv_issuer_qr?.setImageBitmap(state.getIssuerQrCode())
        tv_issuer_address?.text = state.issuerAddress
//        tv_issuer_title?.text = state.issuerName
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }

        btn_issue_credentials.setOnClickListener {
            store.dispatch(IssuerAction.ReadHoldersCard)
        }
    }
}