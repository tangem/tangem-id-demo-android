package com.microsoft.did.sdk.tangem

import com.microsoft.did.sdk.crypto.keyStore.EncryptedKeyStore
import com.microsoft.did.sdk.identifier.IdentifierCreator
import com.microsoft.did.sdk.identifier.models.Identifier
import com.microsoft.did.sdk.util.Constants.MAIN_IDENTIFIER_REFERENCE
import com.microsoft.did.sdk.util.controlflow.Result
import com.microsoft.did.sdk.util.controlflow.runResultTry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class provides methods to create, update and manage decentralized identifiers.
 */
@Singleton
class TangemIdentifierManager @Inject constructor(
    private val identifierCreator: TangemIdentifierCreator,
    private val keyManager: TangemKeyManager
) {
    private var masterIdentifier: Identifier? = null

    suspend fun getMasterIdentifier(): Result<Identifier> {
        return if (masterIdentifier != null) {
            Result.Success(masterIdentifier!!)
        } else {
            createMasterIdentifier()
        }
    }

    private suspend fun createMasterIdentifier(): Result<Identifier> {
        return runResultTry {
            val jwk = keyManager.publicKey
            masterIdentifier = identifierCreator.createIdentifier(MAIN_IDENTIFIER_REFERENCE, jwk, jwk, jwk)
            Result.Success(masterIdentifier!!)
        }
    }
}