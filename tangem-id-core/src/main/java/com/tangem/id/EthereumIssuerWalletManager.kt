package com.tangem.id

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.GasLimit
import com.tangem.blockchain.blockchains.ethereum.TransactionToSign
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class EthereumIssuerWalletManager(
    cardId: String,
    wallet: Wallet,
    private val transactionBuilder: EthereumTransactionBuilder,
    private val networkManager: EthereumNetworkManager
) : WalletManager(cardId, wallet) {

    private val blockchain = wallet.blockchain

    private var pendingTxCount = -1L
    private var txCount = -1L

    private val minAmount = Amount(
        wallet.amounts[AmountType.Coin]!!,
        BigDecimal.ONE.movePointLeft(blockchain.decimals())
    )

    override suspend fun update() {

        val result = networkManager.getInfo(
            wallet.address,
            wallet.amounts[AmountType.Token]?.address,
            wallet.amounts[AmountType.Token]?.decimals
        )
        when (result) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: EthereumInfoResponse) {
        wallet.amounts[AmountType.Coin]?.value = data.balance
        wallet.amounts[AmountType.Token]?.value = data.tokenBalance
        txCount = data.txCount
        pendingTxCount = data.pendingTxCount
        if (txCount == pendingTxCount) {
            wallet.transactions.forEach { it.status = TransactionStatus.Confirmed }
        } else if (wallet.transactions.isEmpty()) {
            wallet.addIncomingTransaction()
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    suspend fun buildTransaction(address: String): Result<TransactionToSign> {
        val fee = when (val result = getFee()) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        val transactionData = createTransaction(minAmount, fee, address)

        return Result.Success(
            transactionBuilder.buildToSign(transactionData, txCount++.toBigInteger())
                ?: return Result.Failure(Exception("Not enough data"))
        )
    }

    suspend fun sendSignedTransaction(
        transaction: TransactionToSign,
        signature: ByteArray
    ): SimpleResult {
        val transactionToSend = transactionBuilder.buildToSend(signature, transaction)
        return networkManager.sendTransaction("0x${transactionToSend.toHexString()}")
    }

    private suspend fun getFee(): Result<Amount> {
        val result = networkManager.getFee(GasLimit.Default.value)
        when (result) {
            is Result.Success -> {
                val feeValues: List<BigDecimal> = result.data
                return Result.Success(Amount(wallet.amounts[AmountType.Coin]!!, result.data[1]))
            }
            is Result.Failure -> return result
        }
    }
}