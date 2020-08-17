package com.tangem.id

import androidx.activity.ComponentActivity
import com.example.tangem_id_core.R
import com.tangem.Log
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.TangemSdkError
import com.tangem.common.CompletionResult
import com.tangem.id.card.ReadFilesTask
import com.tangem.id.card.toVerifiableCredentials
import com.tangem.id.demo.VerifiableDemoCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TangemIdVerifier(
    private val tangemSdk: TangemSdk,
    private val coroutineScope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private val tapHolderCardMessage =
        Message(activity.getString(R.string.sdk_view_delegate_message_holder))

    private var verifierCredentials: List<VerifiableDemoCredential>? = null

    fun getCredentialsPrettyJson(): String {
        return verifierCredentials!!.map { it.verifiableCredential.toPrettyJson() }
            .joinToString("\n")
    }

    fun readCredentialsAsVerifier(callback: (CompletionResult<List<VerifiableDemoCredential>>) -> Unit) {
        tangemSdk.startSessionWithRunnable(
            ReadFilesTask(false),
            initialMessage = tapHolderCardMessage
        ) { result ->
            coroutineScope.launch {
                when (result) {
                    is CompletionResult.Failure -> {
                        if (result.error is TangemSdkError.CardError) {
                            callback(CompletionResult.Failure(TangemIdError.WrongHolderCardType(activity)))
                            return@launch
                        }
                        if (result.error !is TangemSdkError.UserCancelled) {
                            callback(CompletionResult.Failure(TangemIdError.ReadingCardError(activity)))
                        }
                    }
                    is CompletionResult.Success -> {
                        if (result.data.files.isEmpty() || result.data.files[0].fileData.isEmpty()) {
                            callback(CompletionResult.Failure(TangemIdError.NoCredentials(activity)))
                            return@launch
                        }

                        verifierCredentials = result.data.files
                            .toVerifiableCredentials()
                            .mapNotNull {
                                VerifiableDemoCredential.from(
                                    it
//                                    ,
//                                    it.simpleVerify(activity)
                                )
                            }
                        Log.i("Credentials", verifierCredentials.toString())
                        callback(CompletionResult.Success(verifierCredentials!!))
                    }
                }
            }
        }
    }

}
