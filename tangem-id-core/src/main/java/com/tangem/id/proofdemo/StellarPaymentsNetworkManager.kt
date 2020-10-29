package com.tangem.id.proofdemo

import com.google.longrunning.ListOperationsResponse
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.API_STELLAR
import com.tangem.blockchain.network.API_STELLAR_TESTNET
import org.stellar.sdk.Server
import org.stellar.sdk.responses.operations.OperationResponse
import java.net.URISyntaxException

// TODO: add to Blockchain SDK?
class StellarPaymentsNetworkManager(isTestNet: Boolean = false) {
    private val recordsLimitCap = 200

    private val stellarServer by lazy {
        Server(if (isTestNet) API_STELLAR_TESTNET else API_STELLAR)
    }

    suspend fun getPayments(accountId: String): Result<List<OperationResponse>> {
        return try {
            var operationsPage = stellarServer.payments().forAccount(accountId)
                .limit(recordsLimitCap)
                .execute()
            val operations = operationsPage.records

            while (operationsPage.records.size == recordsLimitCap) {
                try {
                    operationsPage = operationsPage.getNextPage(stellarServer.httpClient)
                    operations.addAll(operationsPage.records)
                } catch (e: URISyntaxException) {
                    break
                }
            }
            Result.Success(operations)

        } catch (error: Exception) {
            Result.Failure(error)
        }
    }
}