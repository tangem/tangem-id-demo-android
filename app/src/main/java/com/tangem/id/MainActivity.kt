package com.tangem.id

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.did.sdk.VerifiableCredentialManager
import com.microsoft.did.sdk.VerifiableCredentialSdk
import com.microsoft.did.sdk.credential.service.IssuanceResponse
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
private lateinit var verifiableCredentialManager: VerifiableCredentialManager
private lateinit var issuanceResponse: IssuanceResponse
private lateinit var requestedToken: IdTokenAttestation

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
        CoroutineScope(Job()).launch {
//            val publicKey = "0456C024AFE6CB2C1A384736D5C2AF008C8B6DA7256976C5ADE723E3B05087ED8C58E334A9DC53AA42E3821BA186307C6A9DABED6E8F72612ECFE22FEE0419E0B6".hexToBytes()
//            val identifierString = TangemIonIdentifierCreator().create(publicKey)

            VerifiableCredentialSdk.init(this@MainActivity)
            verifiableCredentialManager = VerifiableCredentialSdk.verifiableCredentialManager
            val identifierManager = VerifiableCredentialSdk.identifierManager

//            val verifiableCredentialManager = VerifiableCredentialSdk.verifiableCredentialManager
//            val tangemVerifiableCredentialManager = TangemVCMFactory.create(cardId, publicKey)
            val identifier = when (val result = identifierManager.getMasterIdentifier()) {
                is MSResult.Success -> result.payload
                is MSResult.Failure -> return@launch
            }

//            val presentationRequest = when (val result = verifiableCredentialManager.getPresentationRequest(
//                "openid://vc/?request_uri=https%3A%2F%2Fvc.tangem.com%2Fissue-request.jwt%3Fid%3D8fmXLQAyvzKB1FpvANlAfDKHq5pyzBOm"
//            )) {
//                is MSResult.Success -> result.payload
//                is MSResult.Failure -> return@runBlocking
//            }

//            val contract = presentationRequest.getPresentationDefinition()
//                .credentialPresentationInputDescriptors[0].issuanceMetadataList[0].issuerContract

            val contract =
                "https://portableidentitycards.azure-api.net/v1.0/9c59be8b-bd18-45d9-b9d9-082bc07c094f/portableIdentities/contracts/Ninja%20Card"

            val issuanceRequest =
                when (val result = verifiableCredentialManager.getIssuanceRequest(contract)) {
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

            issuanceResponse = verifiableCredentialManager.createIssuanceResponse(
                issuanceRequest,
                identifier
            )

            requestedToken = issuanceRequest.getAttestations().idTokens[0]

//            val serviceConfig = AuthorizationServiceConfiguration(
//                Uri.parse("https://verifiablecreds.b2clogin.com/verifiablecreds.onmicrosoft.com/oauth2/v2.0/authorize?p=b2c_1_sisu"), // authorization endpoint
//                Uri.parse("https://verifiablecreds.b2clogin.com/verifiablecreds.onmicrosoft.com/oauth2/v2.0/token?p=b2c_1_sisu") // token endpoint
//            )

            AuthorizationServiceConfiguration.fetchFromUrl(
                Uri.parse(requestedToken.configuration),
                AuthorizationServiceConfiguration
                    .RetrieveConfigurationCallback { serviceConfig, exception ->
                        val authRequestBuilder = AuthorizationRequest.Builder(
                            serviceConfig!!,  // the authorization service configuration
                            requestedToken.client_id,  // the client ID, typically pre-registered and static
                            ResponseTypeValues.ID_TOKEN,  // the response_type value: we want a code
                            Uri.parse(requestedToken.redirect_uri) // the redirect URI to which the auth response is sent
                        )

                        val authRequest = authRequestBuilder
                            .setScope("openid")
                            .setResponseMode("query")
                            .build()

                        doAuthorization(authRequest)
                    }
            )

//            val idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IjdfWnVmMXR2a3dMeFlhSFMzcTZsVWpVWUlHdyIsImtpZCI6IjdfWnVmMXR2a3dMeFlhSFMzcTZsVWpVWUlHdyJ9.eyJhdWQiOiJiMTRhNzUwNS05NmU5LTQ5MjctOTFlOC0wNjAxZDBmYzljYWEiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC9mYTE1ZDY5Mi1lOWM3LTQ0NjAtYTc0My0yOWYyOTU2ZmQ0MjkvIiwiaWF0IjoxNTM2Mjc1MTI0LCJuYmYiOjE1MzYyNzUxMjQsImV4cCI6MTUzNjI3OTAyNCwiYWlvIjoiQVhRQWkvOElBQUFBcXhzdUIrUjREMnJGUXFPRVRPNFlkWGJMRDlrWjh4ZlhhZGVBTTBRMk5rTlQ1aXpmZzN1d2JXU1hodVNTajZVVDVoeTJENldxQXBCNWpLQTZaZ1o5ay9TVTI3dVY5Y2V0WGZMT3RwTnR0Z2s1RGNCdGsrTExzdHovSmcrZ1lSbXY5YlVVNFhscGhUYzZDODZKbWoxRkN3PT0iLCJhbXIiOlsicnNhIl0sImVtYWlsIjoiYWJlbGlAbWljcm9zb2Z0LmNvbSIsImZhbWlseV9uYW1lIjoiTGluY29sbiIsImdpdmVuX25hbWUiOiJBYmUiLCJpZHAiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwiaXBhZGRyIjoiMTMxLjEwNy4yMjIuMjIiLCJuYW1lIjoiYWJlbGkiLCJub25jZSI6IjEyMzUyMyIsIm9pZCI6IjA1ODMzYjZiLWFhMWQtNDJkNC05ZWMwLTFiMmJiOTE5NDQzOCIsInJoIjoiSSIsInN1YiI6IjVfSjlyU3NzOC1qdnRfSWN1NnVlUk5MOHhYYjhMRjRGc2dfS29vQzJSSlEiLCJ0aWQiOiJmYTE1ZDY5Mi1lOWM3LTQ0NjAtYTc0My0yOWYyOTU2ZmQ0MjkiLCJ1bmlxdWVfbmFtZSI6IkFiZUxpQG1pY3Jvc29mdC5jb20iLCJ1dGkiOiJMeGVfNDZHcVRrT3BHU2ZUbG40RUFBIiwidmVyIjoiMS4wIn0=.UJQrCA6qn2bXq57qzGX_-D3HcPHqBMOKDPx4su1yKRLNErVD8xkxJLNLVRdASHqEcpyDctbdHccu6DPpkq5f0ibcaQFhejQNcABidJCTz0Bb2AbdUCTqAzdt9pdgQvMBnVH1xk3SCM6d4BbT4BkLLj10ZLasX7vRknaSjE_C5DI7Fg4WrZPwOhII1dB0HEZ_qpNaYXEiy-o94UJ94zCr07GgrqMsfYQqFR7kn-mn68AjvLcgwSfZvyR_yIK75S_K37vC3QryQ7cNoafDe9upql_6pB2ybMVlgWPs_DmbJ8g0om-sPlwyn74Cc1tW3ze-Xptw_2uVdPgWyqfuWAfq6Q"
//            response.requestedIdTokenMap[requestedToken.configuration] = idToken
//
//
//            val vch = when (val result =
//                verifiableCredentialManager.sendIssuanceResponse(response)) {
//                is MSResult.Success -> result.payload
//                is MSResult.Failure -> return@launch
//            }
        }


    }

    private fun doAuthorization(authRequest: AuthorizationRequest) {
        val authService = AuthorizationService(this)
        val authIntent: Intent = authService.getAuthorizationRequestIntent(authRequest)
        startActivityForResult(authIntent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        runBlocking {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == 0) {
                val resp = AuthorizationResponse.fromIntent(data!!)
                val ex = AuthorizationException.fromIntent(data)

                val idToken =
                    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IjdfWnVmMXR2a3dMeFlhSFMzcTZsVWpVWUlHdyIsImtpZCI6IjdfWnVmMXR2a3dMeFlhSFMzcTZsVWpVWUlHdyJ9.eyJhdWQiOiJiMTRhNzUwNS05NmU5LTQ5MjctOTFlOC0wNjAxZDBmYzljYWEiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC9mYTE1ZDY5Mi1lOWM3LTQ0NjAtYTc0My0yOWYyOTU2ZmQ0MjkvIiwiaWF0IjoxNTM2Mjc1MTI0LCJuYmYiOjE1MzYyNzUxMjQsImV4cCI6MTUzNjI3OTAyNCwiYWlvIjoiQVhRQWkvOElBQUFBcXhzdUIrUjREMnJGUXFPRVRPNFlkWGJMRDlrWjh4ZlhhZGVBTTBRMk5rTlQ1aXpmZzN1d2JXU1hodVNTajZVVDVoeTJENldxQXBCNWpLQTZaZ1o5ay9TVTI3dVY5Y2V0WGZMT3RwTnR0Z2s1RGNCdGsrTExzdHovSmcrZ1lSbXY5YlVVNFhscGhUYzZDODZKbWoxRkN3PT0iLCJhbXIiOlsicnNhIl0sImVtYWlsIjoiYWJlbGlAbWljcm9zb2Z0LmNvbSIsImZhbWlseV9uYW1lIjoiTGluY29sbiIsImdpdmVuX25hbWUiOiJBYmUiLCJpZHAiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwiaXBhZGRyIjoiMTMxLjEwNy4yMjIuMjIiLCJuYW1lIjoiYWJlbGkiLCJub25jZSI6IjEyMzUyMyIsIm9pZCI6IjA1ODMzYjZiLWFhMWQtNDJkNC05ZWMwLTFiMmJiOTE5NDQzOCIsInJoIjoiSSIsInN1YiI6IjVfSjlyU3NzOC1qdnRfSWN1NnVlUk5MOHhYYjhMRjRGc2dfS29vQzJSSlEiLCJ0aWQiOiJmYTE1ZDY5Mi1lOWM3LTQ0NjAtYTc0My0yOWYyOTU2ZmQ0MjkiLCJ1bmlxdWVfbmFtZSI6IkFiZUxpQG1pY3Jvc29mdC5jb20iLCJ1dGkiOiJMeGVfNDZHcVRrT3BHU2ZUbG40RUFBIiwidmVyIjoiMS4wIn0=.UJQrCA6qn2bXq57qzGX_-D3HcPHqBMOKDPx4su1yKRLNErVD8xkxJLNLVRdASHqEcpyDctbdHccu6DPpkq5f0ibcaQFhejQNcABidJCTz0Bb2AbdUCTqAzdt9pdgQvMBnVH1xk3SCM6d4BbT4BkLLj10ZLasX7vRknaSjE_C5DI7Fg4WrZPwOhII1dB0HEZ_qpNaYXEiy-o94UJ94zCr07GgrqMsfYQqFR7kn-mn68AjvLcgwSfZvyR_yIK75S_K37vC3QryQ7cNoafDe9upql_6pB2ybMVlgWPs_DmbJ8g0om-sPlwyn74Cc1tW3ze-Xptw_2uVdPgWyqfuWAfq6Q"
                issuanceResponse.requestedIdTokenMap[requestedToken.configuration] =
                    resp!!.idToken!!


                val vch = when (val result =
                    verifiableCredentialManager.sendIssuanceResponse(issuanceResponse)) {
                    is MSResult.Success -> result.payload
                    is MSResult.Failure -> return@runBlocking
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