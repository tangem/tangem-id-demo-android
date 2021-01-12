package com.tangem.id.features.verifier

import android.os.Bundle
import android.text.Spannable
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.id.R
import com.tangem.id.common.entities.*
import com.tangem.id.common.extensions.*
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.verifier.redux.*
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_issuer.toolbar
import kotlinx.android.synthetic.main.fragment_verifier.*
import kotlinx.android.synthetic.main.layout_checkbox_card.*
import kotlinx.android.synthetic.main.layout_covid.*
import kotlinx.android.synthetic.main.layout_ninja.*
import kotlinx.android.synthetic.main.layout_passport.*
import kotlinx.android.synthetic.main.layout_passport.card_passport
import kotlinx.android.synthetic.main.layout_passport.tv_name
import kotlinx.android.synthetic.main.layout_passport.tv_surname
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

        if (state.jsonShown != null) {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder
                .setMessage(state.jsonShown)
                .setOnDismissListener { store.dispatch(VerifierAction.HideJson) }
                .setPositiveButton(getText(R.string.credential_dialog_share))
                { _, _ -> context?.shareText(state.jsonShown) }
                .setNegativeButton(getText(R.string.credential_dialog_hide)) { dialog, _ -> dialog.cancel() }
            val dialog = builder.create()
            dialog.show()
        }

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
            layout_ssn?.hide()
        } else {
            layout_ssn?.show()
            tv_ssn?.text = state.securityNumber.credential.number
            setCredentialsStatus(
                state.securityNumber.credential,
                state.securityNumber.credentialStatus
            )
            card_ssn?.setMargins()
        }

        if (state.ageOfMajority == null) {
            layout_age_of_majority?.hide()
        } else {
            layout_age_of_majority?.show()
            checkbox?.isChecked = state.ageOfMajority.credential.valid
            checkbox?.isEnabled = false
            setCredentialsStatus(
                state.ageOfMajority.credential,
                state.ageOfMajority.credentialStatus
            )
            card_checkbox?.setMargins()
        }
        if (state.immunityPassport == null) {
            layout_covid?.hide()
        } else {
            layout_covid?.show()
            checkbox_covid?.isChecked = state.immunityPassport.credential.valid
            checkbox_covid?.isEnabled = false
            setCredentialsStatus(
                state.immunityPassport.credential,
                state.immunityPassport.credentialStatus
            )
            card_covid?.setMargins()
        }
        if (state.credentialNinja == null) {
            layout_ninja?.hide()
        } else {
            layout_ninja?.show()
            val credential = state.credentialNinja.credential
            tv_ninja_name?.text = credential.name
            tv_ninja_surname?.text = credential.surname
            setCredentialsStatus(credential, state.credentialNinja.credentialStatus)
            card_ninja?.setMargins()
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
            is ImmunityPassport -> l_credential_status_covid
            is CredentialNinja -> l_credential_status_ninja
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