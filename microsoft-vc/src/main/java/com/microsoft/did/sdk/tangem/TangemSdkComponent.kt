package com.microsoft.did.sdk.tangem

import android.content.Context
import com.microsoft.did.sdk.*
import com.microsoft.did.sdk.di.SdkModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

/**
 * This interface is used by Dagger to generate the code in `DaggerSdkComponent`. It exposes the dependency graph to
 * the outside. Dagger will expose the type inferred by the return type of the interface function.
 *
 * More information:
 * https://dagger.dev/users-guide
 * https://developer.android.com/training/dependency-injection
 */
@Singleton
@Component(modules = [SdkModule::class])
internal interface TangemSdkComponent {

    fun identifierManager(): IdentifierManager

    fun issuanceService(): TangemIssuanceService

    fun presentationService(): TangemPresentationService

    fun revocationService(): RevocationService

    fun linkedDomainsService(): LinkedDomainsService

    fun correlationVectorService(): CorrelationVectorService

    @Component.Builder
    interface Builder {
        fun build(): TangemSdkComponent

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun tangemKeyManager(tangemKeyManager: TangemKeyManager): Builder

        @BindsInstance
        fun resolverUrl(@Named("resolverUrl") resolverUrl: String): Builder

        @BindsInstance
        fun registrationUrl(@Named("registrationUrl") registrationUrl: String): Builder

        @BindsInstance
        fun userAgentInfo(@Named("userAgentInfo") userAgentInfo: String): Builder
    }
}