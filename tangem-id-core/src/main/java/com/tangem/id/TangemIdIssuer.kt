package com.tangem.id

import androidx.activity.ComponentActivity
import com.example.tangem_id_core.R
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.TangemSdkError
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.Card
import com.tangem.commands.Product
import com.tangem.common.CompletionResult
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.documents.VerifiableCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        tangemSdk.scanCard(
            initialMessage = tapIssuerCardMessage
        ) { result ->
            when (result) {
                is CompletionResult.Failure ->
                    if (result.error !is TangemSdkError.UserCancelled) {
                        callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                    }
                is CompletionResult.Success -> {
                    if (result.data.cardData?.productMask?.contains(Product.IdIssuer) != true
                        || !isValidCard(result.data)
                    ) {
                        callback(CompletionResult.Failure(TangemIdError.WrongIssuerCardType(activity)))
                        return@scanCard
                    }

                    issuerWallet = EthereumIssuerWalletManager(result.data)
                    credentialsManager = DemoCredentialsManager(issuerWallet!!, activity, tangemSdk)
                    callback(CompletionResult.Success(credentialsManager!!.issuer))
                }
            }

        }

    }

    private fun isValidCard(card: Card): Boolean {
        return card.walletPublicKey != null
                && card.cardData?.blockchainName == Blockchain.Ethereum.id
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
                    is SimpleResult.Success -> callback(SimpleResponse.Success)
                    is SimpleResult.Failure ->
                        callback(
                            SimpleResponse.Failure(TangemIdError.ErrorWritingCredentials(activity))
                        )
                }
            }
        }
    }

}