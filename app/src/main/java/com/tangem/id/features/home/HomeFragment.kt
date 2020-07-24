package com.tangem.id.features.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_main.*
import java.lang.ref.WeakReference


class HomeFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_issuer.setOnClickListener {
//            store.dispatch(StartingAction.ReadIssuerCard)
            store.dispatch(
                NavigationAction.NavigateTo(AppScreen.Issuer, WeakReference(requireActivity()))
            )
        }
        btn_holder.setOnClickListener {
//            store.dispatch(StartingAction.ReadCredentials)
            store.dispatch(
                NavigationAction.NavigateTo(AppScreen.Holder, WeakReference(requireActivity()))
            )
        }
        btn_verifier.setOnClickListener {
            store.dispatch(
                NavigationAction.NavigateTo(AppScreen.Verifier, WeakReference(requireActivity()))
            )
        }
    }


}