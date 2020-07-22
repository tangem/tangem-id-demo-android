package com.tangem.id.demo

import android.content.Context
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.id.documents.VerifiableCredential
import com.tangem.id.extensions.calculateSha3v512
import com.tangem.id.utils.normalizeJsonLd
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DemoCredentialFactory(
    private val issuer: String,
    private val personData: DemoPersonData,
    subjectId: String,
    private val androidContext: Context
) {
    private val credentialSubjectFactory = DemoCredentialSubjectFactory(subjectId, personData)

    suspend fun createPhotoCredential(): Result<VerifiableCredential> {
        val credentialSubject =
            credentialSubjectFactory.createPhotoCredentialSubject()

        val credential = VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, "TangemPhoto")
        )

        return when (val result = credential.addEthereumId(androidContext)) {
            is SimpleResult.Success -> Result.Success(credential)
            is SimpleResult.Failure -> Result.Failure(result.error)
        }
    }

    suspend fun createPersonalInformationCredential(): Result<VerifiableCredential> {
        val credentialSubject =
            credentialSubjectFactory.createPersonalInformationCredentialSubject()

        val credential = VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
            extraContexts = setOf(TANGEM_DEMO_CONTEXT), // TODO: set actual context
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, "TangemPersonalInformationCredential")
        )

        return when (val result = credential.addEthereumId(androidContext)) {
            is SimpleResult.Success -> Result.Success(credential)
            is SimpleResult.Failure -> Result.Failure(result.error)
        }
    }

    suspend fun createSsnCredential(): Result<VerifiableCredential> {
        val credentialSubject =
            credentialSubjectFactory.createSsnCredentialSubject()

        val credential = VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, "TangemSsnCredential")
        )

        return when (val result = credential.addEthereumId(androidContext)) {
            is SimpleResult.Success -> Result.Success(credential)
            is SimpleResult.Failure -> Result.Failure(result.error)
        }
    }

    suspend fun createAgeOver18Credential(): Result<VerifiableCredential> {
        val credentialSubject =
            credentialSubjectFactory.createSsnCredentialSubject()

        val bornDate = LocalDate.parse(personData.born, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        val over18Date = bornDate.plusYears(18)
        val validFrom = if (over18Date > LocalDate.now()) "${over18Date}T00:00:00Z" else null // TODO: maybe use start of the day in current time zone?

        val credential = VerifiableCredential(
            credentialSubject = credentialSubject,
            issuer = issuer,
            extraContexts = setOf(TANGEM_DEMO_CONTEXT),
            extraTypes = setOf(TANGEM_ETH_CREDENTIAL, "TangemAgeOver18Credential"),
            validFrom = validFrom
        )

        return when (val result = credential.addEthereumId(androidContext)) {
            is SimpleResult.Success -> Result.Success(credential)
            is SimpleResult.Failure -> Result.Failure(result.error)
        }
    }

    companion object {
        const val TANGEM_ETH_CREDENTIAL = "TangemEthCredential"
        const val TANGEM_DEMO_CONTEXT = "https://tangem.com/context/demo" // TODO: set actual context
    }
}

suspend fun VerifiableCredential.addEthereumId(androidContext: Context): SimpleResult {
    val normalizedCredential =
        when (val result = normalizeJsonLd(this.toJSONObject(), androidContext)) {
            is Result.Success -> result.data
            is Result.Failure -> return SimpleResult.Failure(result.error)
        }

    val normalizedCredentialHash = normalizedCredential.toByteArray().calculateSha3v512()
    val credentialEthereumAddress =
        EthereumAddressService().makeAddress(ByteArray(1) + normalizedCredentialHash)
    this.id = "did:ethr:$credentialEthereumAddress"

    return SimpleResult.Success
}

class DemoPersonData(
    val givenName: String,
    val familyName: String,
    val gender: String,
    val born: String,
    val ssn: String,
    val photo: ByteArray
)