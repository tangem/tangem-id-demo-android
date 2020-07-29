package com.tangem.id.features.verifier

import android.os.Bundle
import android.text.Spannable
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.extensions.colorSegment
import com.tangem.id.common.extensions.hide
import com.tangem.id.common.extensions.setMargins
import com.tangem.id.common.extensions.show
import com.tangem.id.common.redux.*
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.verifier.redux.*
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_issuer.toolbar
import kotlinx.android.synthetic.main.fragment_verifier.*
import kotlinx.android.synthetic.main.layout_checkbox_card.*
import kotlinx.android.synthetic.main.layout_passport.*
import kotlinx.android.synthetic.main.layout_photo.*
import kotlinx.android.synthetic.main.layout_ssn.*
import org.rekotlin.StoreSubscriber

class VerifierFragment : Fragment(R.layout.fragment_verifier), StoreSubscriber<VerifierState> {

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

        if (state.photo == null) {
            layout_photo?.hide()
        } else {
            layout_photo?.show()
            state.photo.credential.photo?.let { iv_photo.setImageBitmap(it) }
            setCredentialsStatus(state.photo.credential, state.photo.credentialStatus)
            card_photo?.setMargins()
        }

        if (state.passport == null) {
            layout_passport?.hide()
        } else {
            layout_passport?.show()
            val passport = state.passport.credential
            tv_name?.text = passport.name
            tv_surname?.text = passport.surname
            tv_birth_date?.text = passport.birthDate
            passport.gender?.toLocalizedString()?.let { tv_gender?.text = getString(it) }
            setCredentialsStatus(state.passport.credential, state.passport.credentialStatus)
            card_passport?.setMargins()
        }

        if (state.securityNumber == null) {
            fl_ssn?.hide()
        } else {
            tv_ssn?.text = state.securityNumber.credential.number
            setCredentialsStatus(
                state.securityNumber.credential,
                state.securityNumber.credentialStatus
            )
            card_ssn?.setMargins()
        }

        if (state.ageOfMajority == null) {
            fl_checkbox?.hide()
        } else {
            fl_checkbox?.show()
            checkbox?.isChecked = state.ageOfMajority.credential.valid ?: false
            setCredentialsStatus(
                state.ageOfMajority.credential,
                state.ageOfMajority.credentialStatus
            )
            card_checkbox?.setMargins()
        }
    }

    private fun setCredentialsStatus(credential: Credential, status: CredentialStatus) {
        val statusView = getStatusViewForCredential(credential)
        val tvCredentialStatus =
            statusView?.findViewById<TextView>(R.id.tv_credential_status)
        tvCredentialStatus?.text = getVerificationStatusColoredString(status.verificationStatus)

        val tvIssuer =
            statusView?.findViewById<TextView>(R.id.tv_credential_issuer)
        tvIssuer?.text = getIssuerColoredString(status.issuer)

    }

    private fun getVerificationStatusColoredString(status: VerificationStatus): Spannable {
        val statusString =
            getString(status.getLocalizedStatus())
        val credentialStatus = getString(R.string.verifier_screen_status, statusString)
        val startIndex = credentialStatus.indexOf(statusString)
        val endIndex = startIndex + statusString.length
        return credentialStatus.colorSegment(
            requireContext(), status.getColor(), startIndex, endIndex
        )
    }

    private fun getIssuerColoredString(issuer: Issuer): Spannable {
        val issuerStatus = getString(issuer.isTrustedLocalized())
        val issuerString = getString(
            R.string.verifier_screen_issued_by,
            issuerStatus, issuer.address
        )
        val startIndex = issuerString.indexOf(issuerStatus)
        val endIndex = startIndex + issuerStatus.length
        return issuerString.colorSegment(
            requireContext(), issuer.getColor(), startIndex, endIndex
        )
    }

    private fun getStatusViewForCredential(credential: Credential): View? {
        return when (credential) {
            is Photo -> l_credential_status_photo
            is Passport -> l_credential_status_passport
            is SecurityNumber -> l_credential_status_ssn
            is AgeOfMajority -> l_credential_status_age_of_majority
            else -> null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
        btn_presentation_json?.setOnClickListener { store.dispatch(VerifierAction.ShowJson) }
    }
}