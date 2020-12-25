package com.tangem.id

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.did.sdk.IssuanceService
import com.microsoft.did.sdk.PresentationService
import com.microsoft.did.sdk.VerifiableCredentialSdk
import com.microsoft.did.sdk.credential.service.IssuanceResponse
import com.microsoft.did.sdk.credential.service.PresentationRequest
import com.microsoft.did.sdk.credential.service.PresentationResponse
import com.microsoft.did.sdk.credential.service.models.attestations.IdTokenAttestation
import com.tangem.id.common.redux.NotificationsHandler
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import net.openid.appauth.*
import net.openid.appauth.AuthorizationServiceConfiguration
import java.lang.ref.WeakReference
import com.microsoft.did.sdk.util.controlflow.Result as MSResult


lateinit var tangemIdSdk: TangemIdSdk
var notificationsHandler: NotificationsHandler? = null
private lateinit var issuanceService: IssuanceService
private lateinit var presentationService: PresentationService
private lateinit var presentationRequest: PresentationRequest
private lateinit var issuanceResponse: IssuanceResponse
private lateinit var requestedToken: IdTokenAttestation
private lateinit var serviceConfig: AuthorizationServiceConfiguration
private lateinit var authService: AuthorizationService
private val codeVerifier = "0123456789_0123456789_0123456789_0123456789"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        store.dispatch(NavigationAction.ActivityCreated(WeakReference(this)))

        tangemIdSdk = TangemIdSdk(this)

        testMicrosoftVc()
//        testScan()
    }

    override fun onResume() {
        super.onResume()
        if (supportFragmentManager.backStackEntryCount == 0) {
            store.dispatch(
                NavigationAction.NavigateTo(AppScreen.Home)
            )
        }
        notificationsHandler = NotificationsHandler(fragment_container)
    }

    override fun onStop() {
        notificationsHandler = null
        super.onStop()
    }

    override fun onDestroy() {
        store.dispatch(NavigationAction.ActivityDestroyed)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.change_passcode_menu -> {
                return false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun testMicrosoftVc() {
        authService = AuthorizationService(this)
        CoroutineScope(Job()).launch {
//            val publicKey = "0456C024AFE6CB2C1A384736D5C2AF008C8B6DA7256976C5ADE723E3B05087ED8C58E334A9DC53AA42E3821BA186307C6A9DABED6E8F72612ECFE22FEE0419E0B6".hexToBytes()
//            val identifierString = TangemIonIdentifierCreator().create(publicKey)

            VerifiableCredentialSdk.init(this@MainActivity, "testAgent")
            issuanceService = VerifiableCredentialSdk.issuanceService
            presentationService = VerifiableCredentialSdk.presentationService
//            val identifierManager = VerifiableCredentialSdk.identifierManager

//            val verifiableCredentialManager = VerifiableCredentialSdk.verifiableCredentialManager
//            val tangemVerifiableCredentialManager = TangemVCMFactory.create(cardId, publicKey)
//            val identifier = when (val result = identifierManager.getMasterIdentifier()) {
//                is MSResult.Success -> result.payload
//                is MSResult.Failure -> return@launch
//            }

            presentationRequest = when (val result = presentationService.getRequest(
                "openid://vc/?request_uri=https%3A%2F%2Fvc.tangem.com%2Fissue-request.jwt%3Fid%3DclbQl7GWojUXE5WkIa9VNdDv-5vL-Kgw"
            )) {
                is MSResult.Success -> result.payload
                is MSResult.Failure -> return@launch
            }

            val contract = presentationRequest.getPresentationDefinition()
                .credentialPresentationInputDescriptors[0].issuanceMetadataList[0].issuerContract

//            val contract =
//                "https://portableidentitycards.azure-api.net/v1.0/9c59be8b-bd18-45d9-b9d9-082bc07c094f/portableIdentities/contracts/Ninja%20Card"

            val issuanceRequest =
                when (val result = issuanceService.getRequest(contract)) {
                    is MSResult.Success -> result.payload
                    is MSResult.Failure -> return@launch
                }

//            val pairwiseIdentifier = when (val result = identifierManager.createPairwiseIdentifier(
//                identifier,
//                issuanceRequest.entityIdentifier
//            )) {
//                is MSResult.Success -> result.payload
//                is MSResult.Failure -> return@launch
//            }

            issuanceResponse = IssuanceResponse(issuanceRequest)

            requestedToken = issuanceRequest.getAttestations().idTokens[0]

//            val serviceConfig = AuthorizationServiceConfiguration(
//                Uri.parse("https://verifiablecreds.b2clogin.com/verifiablecreds.onmicrosoft.com/oauth2/v2.0/authorize?p=b2c_1_sisu"), // authorization endpoint
//                Uri.parse("https://verifiablecreds.b2clogin.com/verifiablecreds.onmicrosoft.com/oauth2/v2.0/token?p=b2c_1_sisu") // token endpoint
//            )

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
        }


    }

    private fun doAuthorization(authRequest: AuthorizationRequest) {
        val authIntent: Intent = authService.getAuthorizationRequestIntent(authRequest)
        startActivityForResult(authIntent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
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
                    runBlocking {
                        val verifiableCredential = when (val result =
                            issuanceService.sendResponse(issuanceResponse)) {
                            is MSResult.Success -> result.payload
                            is MSResult.Failure -> return@runBlocking
                        }

                        val presentationResponse = PresentationResponse(presentationRequest)
                        val requestedVc = presentationRequest.getPresentationDefinition()
                            .credentialPresentationInputDescriptors[0]
                        presentationResponse.requestedVcPresentationSubmissionMap[requestedVc] =
                            verifiableCredential

                        val result = presentationService.sendResponse(presentationResponse)
                        print(result)
                    }
            }
        }
    }
//
//    private fun testScan() {
////        val config = Config(
////            cardFilter = CardFilter(EnumSet.allOf(CardType::class.java))
////        )
//        val tangemSdk = tangemIdSdk.tangemSdk
//        val signer = Signer(tangemSdk)
//
//        tangemSdk.scanCard { result ->
//            when (result) {
//                is CompletionResult.Success -> testMicrosoftVc(
//                    signer,
//                    result.data.cardId,
//                    result.data.cardPublicKey!!
//                )
//                is CompletionResult.Failure -> Log.e("test", result.error.customMessage)
//            }
//        }
//    }
}