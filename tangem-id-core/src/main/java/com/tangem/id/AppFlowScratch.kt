package com.tangem.id

import android.content.Context
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.encodeBase64NoWrap
import com.tangem.common.CompletionResult
import com.tangem.id.demo.DemoCredentialFactory
import com.tangem.id.demo.DemoPersonData
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.proof.Secp256k1Proof
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun createDemoCredentials(
    issuerWalletManager: EthereumWalletManager,
    subjectEthereumAddress: String,
    personData: DemoPersonData,
    signer: TransactionSigner,
    androidContext: Context
) : SimpleResult {
    val issuer = "did:ethr:${issuerWalletManager.wallet.address}"

    val credentialFactory = DemoCredentialFactory(
        issuer = issuer,
        personData = personData,
        subjectId = "did:ethr:$subjectEthereumAddress",
        androidContext = androidContext
    )

    return coroutineScope {
        val photoDeferred =
            async { credentialFactory.createPersonalInformationCredential() }
        val personalInformationDeferred =
            async { credentialFactory.createPersonalInformationCredential() }
        val ssnDeferred =
            async { credentialFactory.createSsnCredential() }
        val ageOver18Deferred =
            async { credentialFactory.createAgeOver18Credential() }

        val credentialDefferedList =
            listOf(photoDeferred, personalInformationDeferred, ssnDeferred, ageOver18Deferred)

        val credentialList = mutableListOf<VerifiableCredential>()
        for (deferred in credentialDefferedList) {
            val credential =
                when (val result = deferred.await()) {
                    is Result.Success -> result.data
                    is Result.Failure -> return@coroutineScope SimpleResult.Failure(result.error)
                }
            credentialList.add(credential)
        }

        val hashDeferredList = mutableListOf<Deferred<Result<ByteArray>>>()
        for (credential in credentialList) {
            val proof = Secp256k1Proof("$issuer#owner")
            credential.proof = proof

            val hashDeferred = async { proof.calculateHashToSign(credential, androidContext) }
            hashDeferredList.add(hashDeferred)
        }

        val credentialHashList = mutableListOf<ByteArray>()
        for (deferred in hashDeferredList) {
            val credentialHash =
                when (val result = deferred.await()) {
                    is Result.Success -> result.data
                    is Result.Failure -> return@coroutineScope SimpleResult.Failure(result.error)
                }
            credentialHashList.add(credentialHash)
        }

        val transactionHashList = mutableListOf<ByteArray>()
        // TODO: fill the list with transaction hashes for every credential where amount is minimal and destination is credential id

        val arrayToSign = (credentialHashList + transactionHashList).toTypedArray()

        //TODO: Sign and split signatures
        val credentialSignaturesList = listOf<ByteArray>()
        val trensactionSignaturesList = listOf<ByteArray>()

        for (index in credentialList.indices) {
            val credential = credentialList[index]
            val secp256k1Proof = credential.proof as Secp256k1Proof
            secp256k1Proof.addSignature(credentialSignaturesList[index])
        }

        //TODO: send transactions and write credentials on card

        SimpleResult.Success
    }
}