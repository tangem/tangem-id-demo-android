package com.tangem.id.features.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.extensions.colorSegment
import com.tangem.id.features.home.redux.HomeAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_main.*


class HomeFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_issuer.setOnClickListener { store.dispatch(HomeAction.ReadIssuerCard) }
        btn_holder.setOnClickListener { store.dispatch(HomeAction.ReadCredentialsAsHolder) }
        btn_verifier.setOnClickListener { store.dispatch(HomeAction.ReadCredentialsAsVerifier) }

        setCardStoreLink(requireContext())
    }

    private fun setCardStoreLink(context: Context) {
        val text = getString(
            R.string.starting_screen_description, getString(R.string.starting_screen_store_address)
        )
        val spannable = text.colorSegment(context, R.color.colorAccent,
            text.indexOf(getString(R.string.starting_screen_store_address)))

        tv_app_description?.setText(spannable, TextView.BufferType.SPANNABLE)
        tv_app_description?.setOnClickListener {
            val uri = Uri.parse(CARD_SHOP_URI)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    companion object {
        const val CARD_SHOP_URI = "https://shop.tangem.com/"
    }
}