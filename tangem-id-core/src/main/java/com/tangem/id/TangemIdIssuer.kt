package com.tangem.id

import android.util.Log
import androidx.activity.ComponentActivity
import com.example.tangem_id_core.R
import com.microsoft.did.sdk.VerifiableCredentialSdk
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.TangemSdkError
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Signer
import com.tangem.commands.Card
import com.tangem.commands.EllipticCurve
import com.tangem.commands.Product
import com.tangem.common.CompletionResult
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.documents.VerifiableCredential
//import com.tangem.id.features.microsoft.TangemVCMFactory
import kotlinx.coroutines.*

class TangemIdIssuer(
    private val tangemSdk: TangemSdk,
    private val coroutineScope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private var issuerWallet: EthereumIssuerWalletManager? = null
    private var credentialsManager: DemoCredentialsManager? = null
    private var holderCardId: String? = null

    private var credentials: List<VerifiableCredential>? = null

    private val tapHolderCardMessage =
        Message(activity.getString(R.string.sdk_view_delegate_message_holder))
    private val tapIssuerCardMessage =
        Message(activity.getString(R.string.sdk_view_delegate_message_issuer))


    fun showJsonWhileCreating(): String {
        return credentialsManager!!.showCredentials().joinToString("\n")
    }

    fun readIssuerCard(callback: (CompletionResult<String>) -> Unit) {
//        tangemSdk.scanCard(
//            initialMessage = tapIssuerCardMessage
//        ) { result ->
//            when (result) {
//                is CompletionResult.Failure ->
//                    if (result.error !is TangemSdkError.UserCancelled) {
//                        callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
//                    }
//                is CompletionResult.Success -> {
//                    if (result.data.cardData?.productMask?.contains(Product.IdIssuer) != true
//                        || !isValidCard(result.data)
//                    ) {
//                        callback(CompletionResult.Failure(TangemIdError.WrongIssuerCardType(activity)))
//                        return@scanCard
//                    }
//
//                    issuerWallet = EthereumIssuerWalletManager(result.data)
//                    credentialsManager = DemoCredentialsManager(issuerWallet!!, activity, tangemSdk)
//                    callback(CompletionResult.Success(credentialsManager!!.issuer))
//                }
//            }
//
//        }
//        testScan()
    }

//    private fun testMicrosoftVc(signer: TransactionSigner, cardId: String, publicKey: ByteArray) {
//        runBlocking {
////            val publicKey = "0456C024AFE6CB2C1A384736D5C2AF008C8B6DA7256976C5ADE723E3B05087ED8C58E334A9DC53AA42E3821BA186307C6A9DABED6E8F72612ECFE22FEE0419E0B6".hexToBytes()
////            val identifierString = TangemIonIdentifierCreator().create(publicKey)
//
//            VerifiableCredentialSdk.init(activity)
//
//            val verifiableCredentialManager = VerifiableCredentialSdk.verifiableCredentialManager
//            val tangemVerifiableCredentialManager = TangemVCMFactory.create(cardId, publicKey)
//////            val decoded = URLDecoder.decode("https%3A%2F%2F2e036ba234b6.ngrok.io%2Fissue-request.jwt%3Fid%3DPL7Psq3odm_-Hx8eVbNffrxt6CXPRE0U")
////            val identifier = when (val result = identifierManager.getMasterIdentifier()) {
////                is Result.Success -> result.payload
////                is Result.Failure -> return@runBlocking
////            }
//
////            val presentationRequest = when (val result = verifiableCredentialManager.getPresentationRequest(
////                "openid://vc/?request_uri=https%3A%2F%2Fvc.tangem.com%2Fissue-request.jwt%3Fid%3DpXIS0Q95wdvV4Jv20bzi_WT3_HcmzpQU"
////            )) {
////                is Result.Success -> result.payload
////                is Result.Failure -> return@runBlocking
////            }
////
////            val contract = presentationRequest.content.attestations.presentations[0].contracts[0]
//
//            val contract =
//                "https://portableidentitycards.azure-api.net/v1.0/9c59be8b-bd18-45d9-b9d9-082bc07c094f/portableIdentities/contracts/Ninja%20Card"
//
//            val issuanceRequest =
//                when (val result = verifiableCredentialManager.getIssuanceRequest(contract)) {
//                    is com.microsoft.did.sdk.util.controlflow.Result.Success -> result.payload
//                    is com.microsoft.did.sdk.util.controlflow.Result.Failure -> return@runBlocking
//                }
//            val response = verifiableCredentialManager.createIssuanceResponse(issuanceRequest)
//
////            val idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IjdfWnVmMXR2a3dMeFlhSFMzcTZsVWpVWUlHdyIsImtpZCI6IjdfWnVmMXR2a3dMeFlhSFMzcTZsVWpVWUlHdyJ9.eyJhdWQiOiJiMTRhNzUwNS05NmU5LTQ5MjctOTFlOC0wNjAxZDBmYzljYWEiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC9mYTE1ZDY5Mi1lOWM3LTQ0NjAtYTc0My0yOWYyOTU2ZmQ0MjkvIiwiaWF0IjoxNTM2Mjc1MTI0LCJuYmYiOjE1MzYyNzUxMjQsImV4cCI6MTUzNjI3OTAyNCwiYWlvIjoiQVhRQWkvOElBQUFBcXhzdUIrUjREMnJGUXFPRVRPNFlkWGJMRDlrWjh4ZlhhZGVBTTBRMk5rTlQ1aXpmZzN1d2JXU1hodVNTajZVVDVoeTJENldxQXBCNWpLQTZaZ1o5ay9TVTI3dVY5Y2V0WGZMT3RwTnR0Z2s1RGNCdGsrTExzdHovSmcrZ1lSbXY5YlVVNFhscGhUYzZDODZKbWoxRkN3PT0iLCJhbXIiOlsicnNhIl0sImVtYWlsIjoiYWJlbGlAbWljcm9zb2Z0LmNvbSIsImZhbWlseV9uYW1lIjoiTGluY29sbiIsImdpdmVuX25hbWUiOiJBYmUiLCJpZHAiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwiaXBhZGRyIjoiMTMxLjEwNy4yMjIuMjIiLCJuYW1lIjoiYWJlbGkiLCJub25jZSI6IjEyMzUyMyIsIm9pZCI6IjA1ODMzYjZiLWFhMWQtNDJkNC05ZWMwLTFiMmJiOTE5NDQzOCIsInJoIjoiSSIsInN1YiI6IjVfSjlyU3NzOC1qdnRfSWN1NnVlUk5MOHhYYjhMRjRGc2dfS29vQzJSSlEiLCJ0aWQiOiJmYTE1ZDY5Mi1lOWM3LTQ0NjAtYTc0My0yOWYyOTU2ZmQ0MjkiLCJ1bmlxdWVfbmFtZSI6IkFiZUxpQG1pY3Jvc29mdC5jb20iLCJ1dGkiOiJMeGVfNDZHcVRrT3BHU2ZUbG40RUFBIiwidmVyIjoiMS4wIn0=.UJQrCA6qn2bXq57qzGX_-D3HcPHqBMOKDPx4su1yKRLNErVD8xkxJLNLVRdASHqEcpyDctbdHccu6DPpkq5f0ibcaQFhejQNcABidJCTz0Bb2AbdUCTqAzdt9pdgQvMBnVH1xk3SCM6d4BbT4BkLLj10ZLasX7vRknaSjE_C5DI7Fg4WrZPwOhII1dB0HEZ_qpNaYXEiy-o94UJ94zCr07GgrqMsfYQqFR7kn-mn68AjvLcgwSfZvyR_yIK75S_K37vC3QryQ7cNoafDe9upql_6pB2ybMVlgWPs_DmbJ8g0om-sPlwyn74Cc1tW3ze-Xptw_2uVdPgWyqfuWAfq6Q"
////            response.addRequestedIdToken(issuanceRequest.attestations.idTokens[0], idToken)
//
////            val pairwiseIdentifier = when (val result = identifierManager.createPairwiseIdentifier(identifier, issuanceRequest.entityIdentifier)) {
////                is Result.Success -> result.payload
////                is Result.Failure -> return@runBlocking
////            }
//
//            val verifiableCredential = when (val result =
//                tangemVerifiableCredentialManager.sendIssuanceResponse(response, signer)) {
//                is Result.Success -> result.data
//                is Result.Failure -> return@runBlocking
//            }
//
//            val x = 0
//            // create issuance response.
////            val response = verifiableCredentialManager.createIssuanceResponse(request)
//        }
//
//
////// add requested verifiable credentials to response.
////addCollectedRequirementsToResponse(response, requirementList)
////// get Master Identifier.
////val identifier = identifierManager.getMasterIdentifier()
////// create a pairwise identifier for connection from master identifier and requester's identifier.
////val pairwiseIdentifier = identifierManager.createPairwiseIdentifier(identifier, request.entityIdentifier)
////// send issuance response in order to get a verifiable credential, signed by pairwise identifier.
////val vch = verifiableCredentialManager.sendIssuanceResponse(response, pairwiseIdentifier)
////// save vc to database.
////verifiableCredentialManager.saveVch(vch)
//    }
//
//    private fun testScan() {
////        val config = Config(
////            cardFilter = CardFilter(EnumSet.allOf(CardType::class.java))
////        )
//
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

    private fun isValidCard(card: Card): Boolean {
        return card.walletPublicKey != null && card.curve == EllipticCurve.Secp256k1
    }


    fun getHolderAddress(callback: (CompletionResult<String>) -> Unit) {
        tangemSdk.scanCard(
            initialMessage = tapHolderCardMessage
        ) { result ->
            when (result) {
                is CompletionResult.Failure ->
                    if (result.error !is TangemSdkError.UserCancelled) {
                        callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                    }
                is CompletionResult.Success -> {
                    if (result.data.cardData?.productMask?.contains(Product.IdCard) != true
                        || result.data.walletPublicKey == null
                    ) {
                        callback(CompletionResult.Failure(TangemIdError.WrongHolderCardType(activity)))
                        return@scanCard
                    }
                    holderCardId = result.data.cardId
                    val holderAddress =
                        EthereumAddressService().makeAddress(result.data.walletPublicKey!!)
                    callback(CompletionResult.Success(holderAddress))
                }
            }
        }
    }


    fun formCredentialsAndSign(
        personData: DemoPersonData, holdersAddress: String,
        callback: (SimpleResponse) -> Unit
    ) {
        coroutineScope.launch {
            val result = credentialsManager!!
                .createDemoCredentials(personData, holdersAddress, tapIssuerCardMessage)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        credentials = result.data
                        callback(SimpleResponse.Success)
                    }
                    is Result.Failure ->
                        if (result.error is TangemIdError.UserCancelled) {
                            callback(SimpleResponse.Failure(TangemIdError.UserCancelled(activity)))
                        } else {
                            callback(
                                SimpleResponse.Failure(
                                    TangemIdError.ErrorCreatingCredentials(activity)
                                )
                            )
                        }
                }
            }
        }
    }

    fun writeCredentialsAndSend(callback: (SimpleResponse) -> Unit) {
        coroutineScope.launch {
            val result = credentialsManager!!.completeWithId(
                credentials!!, tangemSdk, tapHolderCardMessage, holderCardId!!
            )
            withContext(Dispatchers.Main) {
                when (result) {
                    is SimpleResponse.Success -> callback(SimpleResponse.Success)
                    is SimpleResponse.Failure ->
                        if (result.error is TangemSdkError.UserCancelled) {
                            callback(SimpleResponse.Failure(TangemIdError.UserCancelled(activity)))
                        } else {
                            callback(
                                SimpleResponse.Failure(
                                    TangemIdError.ErrorWritingCredentials(activity)
                                )
                            )
                        }
                }
            }
        }
    }

}