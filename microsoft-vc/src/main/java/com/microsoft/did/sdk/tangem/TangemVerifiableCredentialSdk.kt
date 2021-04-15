package com.microsoft.did.sdk.tangem

import android.content.Context
import com.microsoft.did.sdk.*
import com.microsoft.did.sdk.di.DaggerSdkComponent
import com.microsoft.did.sdk.util.log.DefaultLogConsumer
import com.microsoft.did.sdk.util.log.SdkLog

/**
 * This class initializes the VerifiableCredentialSdk. The `init` method has to be called before the members can be accessed.
 * Call the init method as soon as possible, for example in the `onCreate()` method of your `Application` implementation.
 * An Android context and user agent information (i.e, name/version) have to be provided as such:
 *
 * VerifiableCredentialSdk.init(getApplicationContext(), "");
 *
 * The `VerifiableCredentialManager` can be accessed through this static reference, but ideally should be provided
 * by your own dependency injection library. In the case of Dagger2 as such:
 *
 * @Provides
 * fun provideIssuanceService(): IssuanceService {
 *     return VerifiableCredentialSdk.issuanceService
 * }
 */
object TangemVerifiableCredentialSdk {

    @JvmStatic
    lateinit var issuanceService: TangemIssuanceService

    @JvmStatic
    lateinit var presentationService: TangemPresentationService

    @JvmStatic
    lateinit var revocationService: RevocationService

    @JvmStatic
    lateinit var correlationVectorService: CorrelationVectorService

    @JvmStatic
    internal lateinit var identifierManager: IdentifierManager

    /**
     * Initializes VerifiableCredentialSdk
     *
     * @param context context instance
     * @param userAgentInfo it contains name and version of the client. It will be used in User-Agent header for all the requests.
     * @param logConsumer logger implementation to be used
     * @param registrationUrl url used to register DID
     * @param resolverUrl url used to resolve DID
     */
    @JvmOverloads
    @JvmStatic
    fun init(
        context: Context,
        userAgentInfo: String,
        tangemKeyManager: TangemKeyManager,
        logConsumer: SdkLog.Consumer = DefaultLogConsumer(),
        registrationUrl: String = "",
        resolverUrl: String = "https://beta.discover.did.microsoft.com/1.0/identifiers"
    ) {
        val sdkComponent = DaggerTangemSdkComponent.builder()
            .context(context)
            .userAgentInfo(userAgentInfo)
            .registrationUrl(registrationUrl)
            .resolverUrl(resolverUrl)
            .tangemKeyManager(tangemKeyManager)
            .build()

        issuanceService = sdkComponent.issuanceService()
        presentationService = sdkComponent.presentationService()
        revocationService = sdkComponent.revocationService()
        correlationVectorService = sdkComponent.correlationVectorService()
        identifierManager = sdkComponent.identifierManager()

        correlationVectorService.startNewFlowAndSave()

        SdkLog.addConsumer(logConsumer)
    }
}