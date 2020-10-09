//package com.tangem.id.features.microsoft
//
//import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
//import com.microsoft.did.sdk.credential.models.VerifiableCredential
//import com.microsoft.did.sdk.credential.service.IssuanceResponse
//import com.microsoft.did.sdk.datasource.network.apis.ApiProvider
//import com.microsoft.did.sdk.datasource.network.credentialOperations.SendVerifiableCredentialIssuanceRequestNetworkOperation
//import com.microsoft.did.sdk.util.Constants
//import com.microsoft.did.sdk.util.serializer.Serializer
//import com.microsoft.did.sdk.util.unwrapSignedVerifiableCredential
//import com.tangem.blockchain.common.TransactionSigner
//import com.tangem.blockchain.extensions.Result
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import okhttp3.OkHttpClient
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.converter.scalars.ScalarsConverterFactory
//import com.microsoft.did.sdk.util.controlflow.Result as MSResult
//
//class TangemVerifiableCredentialManager(
//    val formatter: TangemOidcResponseFormatter,
//    val serializer: Serializer
//) {
//
//    val okHttpClient = OkHttpClient()
//        .newBuilder()
////        .addInterceptor(HttpLoggingInterceptor { SdkLog.d(it) })
//        .build()
//    val retrofit = Retrofit.Builder()
//        .baseUrl("http://TODO.me")
//        .client(okHttpClient)
//        .addConverterFactory(ScalarsConverterFactory.create())
//        .addConverterFactory(GsonConverterFactory.create())
//        .addCallAdapterFactory(CoroutineCallAdapterFactory())
//        .build()
//    val apiProvider = ApiProvider(retrofit)
//
//    suspend fun sendIssuanceResponse(
//        response: IssuanceResponse,
//        signer: TransactionSigner,
//        expiryInSeconds: Int = Constants.DEFAULT_EXPIRATION_IN_SECONDS
//    ): Result<VerifiableCredential> {
//        return withContext(Dispatchers.IO) {
//            try {
//                val requestedVchMap = response.getRequestedVchs()
//
//                val formattedResponseResult = formatter.formatAndSign(
//                    responseAudience = response.audience,
//                    presentationsAudience = response.request.entityIdentifier,
//                    requestedVchMap = requestedVchMap,
//                    requestedIdTokenMap = response.getRequestedIdTokens(),
//                    requestedSelfAttestedClaimMap = response.getRequestedSelfAttestedClaims(),
//                    contract = response.request.contractUrl,
//                    expiryInSeconds = expiryInSeconds,
//                    signer = signer
//                )
//
//                val formattedResponse = when (formattedResponseResult) {
//                    is Result.Success -> formattedResponseResult.data
//                    is Result.Failure -> return@withContext Result.Failure(formattedResponseResult.error)
//                }
//
//                val rawVerifiableCredentialResult =
//                    SendVerifiableCredentialIssuanceRequestNetworkOperation(
//                        response.audience,
//                        formattedResponse,
//                        apiProvider
//                    ).fire()
//
//                return@withContext when (rawVerifiableCredentialResult) {
//                    is MSResult.Success -> Result.Success(
//                        formVerifiableCredential(
//                            rawVerifiableCredentialResult.payload
//                        )
//                    )
//                    is MSResult.Failure -> Result.Failure(rawVerifiableCredentialResult.payload)
//                }
//            } catch (exception: Exception) {
//                return@withContext Result.Failure(exception)
//            }
//        }
//    }
//
//    private fun formVerifiableCredential(
//        rawToken: String,
//        vcId: String? = null
//    ): VerifiableCredential {
//        val vcContents = unwrapSignedVerifiableCredential(rawToken, serializer)
//        return VerifiableCredential(vcContents.jti, rawToken, vcContents, vcId ?: vcContents.jti)
//    }
//}