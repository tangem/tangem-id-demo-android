package com.tangem.id.features.holder.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.microsoft.did.sdk.IssuanceService
import com.microsoft.did.sdk.PresentationService
import com.microsoft.did.sdk.VerifiableCredentialSdk
import com.microsoft.did.sdk.credential.service.IssuanceResponse
import com.microsoft.did.sdk.credential.service.PresentationRequest
import com.microsoft.did.sdk.credential.service.PresentationResponse
import com.microsoft.did.sdk.credential.service.models.attestations.IdTokenAttestation
import com.tangem.common.CompletionResult
import com.tangem.id.R
import com.tangem.id.TangemIdError
import com.tangem.id.common.redux.AppState
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.holder.redux.HolderAction
import com.tangem.id.features.holder.redux.toHolderCredential
import com.tangem.id.store
import com.tangem.id.tangemIdSdk
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_request_credentials.*
import kotlinx.coroutines.*
import net.openid.appauth.*
import net.openid.appauth.AuthorizationServiceConfiguration
import org.rekotlin.Action
import org.rekotlin.StoreSubscriber
import com.microsoft.did.sdk.util.controlflow.Result as MSResult

// TODO: remove logic from fragment
class RequestCredentialsFragment : Fragment(R.layout.fragment_request_credentials),
    StoreSubscriber<RequestCredentialsState> {

    private val mainThread = Handler(Looper.getMainLooper())
    private val coroutineScope = tangemIdSdk.holder.coroutineScope
    private lateinit var issuanceService: IssuanceService
    private lateinit var presentationService: PresentationService
    private lateinit var presentationRequest: PresentationRequest
    private lateinit var issuanceResponse: IssuanceResponse
    private lateinit var requestedToken: IdTokenAttestation
    private lateinit var serviceConfig: AuthorizationServiceConfiguration
    private lateinit var authService: AuthorizationService
    private val codeVerifier = "0123456789_0123456789_0123456789_0123456789"
    private var requestInProgress = false

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
                oldState.requestCredentialsState == newState.requestCredentialsState
            }.select { it.requestCredentialsState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: RequestCredentialsState) {
        if (activity == null || state.requestUri == null || requestInProgress) return
        requestInProgress = true
        requestMicrosoftCredential(state.requestUri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
    }

    private fun requestMicrosoftCredential(requestUri: String) {
        authService = AuthorizationService(requireContext())
        coroutineScope.launch {
            try {
                VerifiableCredentialSdk.init(requireContext(), "testAgent")
                issuanceService = VerifiableCredentialSdk.issuanceService
                presentationService = VerifiableCredentialSdk.presentationService

//                val issuanceRequest = when (val result = issuanceService.getRequest(requestUri)) {
//                    is MSResult.Success -> result.payload
//                    is MSResult.Failure -> return@launch
//                }

                presentationRequest =
                    when (val result = presentationService.getRequest(requestUri)) {
                        is MSResult.Success -> result.payload
                        is MSResult.Failure -> return@launch
                    }

                presentationRequest = PresentationRequest(
                    presentationRequest.content.copy(audience = presentationRequest.content.clientId!!),
                    presentationRequest.linkedDomainResult
                )

            val contract = presentationRequest.getPresentationDefinition()
                .credentialPresentationInputDescriptors[0].issuanceMetadataList[0].issuerContract
//                val contract =
//                    "https://portableidentitycards.azure-api.net/v1.0/3c32ed40-8a10-465b-8ba4-0b1e86882668/portableIdentities/contracts/VerifiedCredentialNinja"

                val issuanceRequest =
                    when (val result = issuanceService.getRequest(contract)) {
                        is MSResult.Success -> result.payload
                        is MSResult.Failure -> return@launch
                    }

                issuanceResponse = IssuanceResponse(issuanceRequest)

                requestedToken = issuanceRequest.getAttestations().idTokens[0]

                AuthorizationServiceConfiguration.fetchFromUrl(
                    Uri.parse(requestedToken.configuration)
                ) { _serviceConfig, exception ->
                    serviceConfig = _serviceConfig!!
                    val authRequestBuilder = AuthorizationRequest.Builder(
                        serviceConfig!!,  // the authorization service configuration
                        requestedToken.client_id,  // the client ID, typically pre-registered and static
                        ResponseTypeValues.CODE,  // the response_type value: we want a code
                        Uri.parse(requestedToken.redirect_uri) // the redirect URI to which the auth response is sent
                    )

                    val authRequest = authRequestBuilder
                        .setScope("openid")
                        .setResponseMode("query")
                        .setCodeVerifier("0123456789_0123456789_0123456789_0123456789")
                        .build()

                    doAuthorization(authRequest)
                }
            } catch (exception: Exception) {
                completeWithError()
            }
        }
    }

    private fun doAuthorization(authRequest: AuthorizationRequest) {
        val authIntent: Intent = authService.getAuthorizationRequestIntent(authRequest)
        startActivityForResult(authIntent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        tv_progress_message?.text = getString(R.string.request_credentials_status_issue)
        if (requestCode == 0) {
            val resp = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)

            val code = resp?.authorizationCode
            val tokenRequestBuilder = TokenRequest.Builder(
                serviceConfig!!,  // the authorization service configuration
                requestedToken.client_id,  // the client ID, typically pre-registered and static
            )

            val tokenRequest = tokenRequestBuilder
                .setScope("openid")
                .setAuthorizationCode(code)
                .setGrantType("authorization_code")
                .setRedirectUri(Uri.parse(requestedToken.redirect_uri))
                .setCodeVerifier(codeVerifier)
                .build()

            authService.performTokenRequest(tokenRequest) { response, exception ->
                issuanceResponse.requestedIdTokenMap[requestedToken.configuration] =
                    response!!.idToken!!
                coroutineScope.launch {
                    try {
                        val verifiableCredential = when (val result =
                            issuanceService.sendResponse(issuanceResponse)) {
                            is MSResult.Success -> result.payload
                            is MSResult.Failure -> throw result.payload
                        }

                        val presentationResponse = PresentationResponse(presentationRequest)
                        val requestedVc = presentationRequest.getPresentationDefinition()
                            .credentialPresentationInputDescriptors[0]
                        presentationResponse.requestedVcPresentationSubmissionMap[requestedVc] =
                            verifiableCredential

                        tangemIdSdk.holder.addNinjaCredential(
                            verifiableCredential,
                            store.state.holderState.cardId
                        ) { result ->
                            mainThread.post {
                                when (result) {
                                    is CompletionResult.Success -> {
                                        CoroutineScope(Job()).launch {
                                            presentationService.sendResponse(
                                                presentationResponse
                                            )
                                        }
                                        val holdersCredentials =
                                            result.data.map { it.toHolderCredential() }
                                        store.dispatch(
                                            HolderAction.RequestNewCredential.Success(
                                                holdersCredentials
                                            )
                                        )
                                    }
                                    is CompletionResult.Failure ->
                                        store.dispatch(
                                            HolderAction.RequestNewCredential.Failure(
                                                result.error
                                            )
                                        )
                                }
//                            store.dispatch(NavigationAction.PopBackTo(AppScreen.Holder)) //TODO: why doesn't work?
                                store.dispatch(NavigationAction.PopBackTo())
                                store.dispatch(NavigationAction.PopBackTo())
                            }
                        }
                    } catch (exception: Exception) {
                        completeWithError()
                    }
                }
            }
        }
    }

    private fun completeWithError() {
        mainThread.post {
            store.dispatch(
                HolderAction.RequestNewCredential.Failure(
                    TangemIdError.ErrorRequestingCredentials(requireActivity())
                )
            )
            store.dispatch(NavigationAction.PopBackTo())
            store.dispatch(NavigationAction.PopBackTo())
        }
    }
}

data class RequestCredentialsState(
    val requestUri: String? = null
)

fun requestCredentialsReducer(action: Action, state: AppState): RequestCredentialsState {
    return if (action is HolderAction.RequestNewCredential) {
        state.requestCredentialsState.copy(requestUri = action.requestUri)
    } else {
        state.requestCredentialsState
    }
}