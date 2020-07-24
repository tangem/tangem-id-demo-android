package com.tangem.id.features.verifier

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.verifier.redux.VerifierState
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_issuer.*
import org.rekotlin.StoreSubscriber

class VerifierFragment : Fragment(R.layout.fragment_issuer), StoreSubscriber<VerifierState> {

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
                oldState.verifierState == newState.verifierState
            }.select { it.verifierState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: VerifierState) {
        if (activity == null) return
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener{
            store.dispatch(NavigationAction.PopBackTo(activity = requireActivity()))
        }
    }
}