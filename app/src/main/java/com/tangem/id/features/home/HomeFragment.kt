package com.tangem.id.features.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_main.*


class HomeFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_issuer.setOnClickListener {
            store.dispatch(HomeAction.ReadIssuerCard)
//            store.dispatch(NavigationAction.NavigateTo(AppScreen.Issuer))

        }
        btn_holder.setOnClickListener {
//            store.dispatch(HomeAction.ReadCredentialsAsHolder)
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Holder))

        }
        btn_verifier.setOnClickListener {
//            store.dispatch(HomeAction.ReadCredentialsAsVerifier)
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Verifier))

        }
    }


}