package com.tangem.id.features.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.features.home.redux.HomeAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_main.*


class HomeFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_issuer.setOnClickListener {
            store.dispatch(HomeAction.ReadIssuerCard)

        }
        btn_holder.setOnClickListener {
            store.dispatch(HomeAction.ReadCredentialsAsHolder)

        }
        btn_verifier.setOnClickListener {
            store.dispatch(HomeAction.ReadCredentialsAsVerifier)
        }
    }


}