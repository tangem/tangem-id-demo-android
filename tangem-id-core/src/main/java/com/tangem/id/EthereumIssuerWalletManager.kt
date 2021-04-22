package com.tangem.id

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.TransactionToSign
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.common.card.Card
import com.tangem.common.extensions.toHexString
import org.kethereum.DEFAULT_GAS_LIMIT
import java.math.BigDecimal

class EthereumIssuerWalletManager(
    cardId: String,
    wallet: Wallet,
    private val transactionBuilder: EthereumTransactionBuilder,
    private val networkProvider: EthereumNetworkProvider
) : WalletManager(cardId, wallet) {

    constructor(card: Card) : this(
        card.cardId,
        Wallet(Blockchain.Ethereum, Blockchain.Ethereum.makeAddresses(card.walletPublicKey!!), emptySet()),
        EthereumTransactionBuilder(card.walletPublicKey!!, Blockchain.Ethereum),
        EthereumNetworkService(Blockchain.Ethereum, "613a0b14833145968b1f656240c7d245")
    )

    private val blockchain = wallet.blockchain

    private var pendingTxCount = -1L
    private var txCount = -1L

    private val minAmount = Amount(
        wallet.amounts[AmountType.Coin]!!,
        BigDecimal.ONE.movePointLeft(blockchain.decimals())
    )

    override suspend fun update() {

        val result = networkProvider.getInfo(wallet.address, emptySet())
        when (result) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: EthereumInfoResponse) {
        wallet.amounts[AmountType.Coin]?.value = data.coinBalance
        txCount = data.txCount
        pendingTxCount = data.pendingTxCount
        if (txCount == pendingTxCount) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        } else if (wallet.recentTransactions.isEmpty()) {
            wallet.addTransactionDummy(TransactionDirection.Incoming)
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    suspend fun buildTransaction(address: String): Result<TransactionToSign> {
        val fee = when (val result = getFee(address)) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        val transactionData = createTransaction(minAmount, fee[1], address)

        return Result.Success(
            transactionBuilder.buildToSign(transactionData, (txCount + 1).toBigInteger())
                ?: return Result.Failure(Exception("Not enough data"))
        )
    }

    suspend fun sendSignedTransaction(
        transaction: TransactionToSign,
        signature: ByteArray
    ): SimpleResult {
        val transactionToSend = transactionBuilder.buildToSend(signature, transaction)
        return networkProvider.sendTransaction("0x${transactionToSend.toHexString()}")
    }

    private suspend fun getFee(destination: String): Result<List<Amount>> {
        var to = destination
        val from = wallet.address
        var data: String? = null
        val fallbackGasLimit = DEFAULT_GAS_LIMIT.toLong()

        return when (val result = networkProvider.getFee(to, from, data, fallbackGasLimit)) {
            is Result.Success -> {
                val feeValues: List<BigDecimal> = result.data.fees
                Result.Success(
                    feeValues.map { feeValue -> Amount(wallet.amounts[AmountType.Coin]!!, feeValue) })
            }
            is Result.Failure -> result
        }
    }

    private fun estimateGasLimit(amount: Amount): EthereumWalletManager.GasLimit { //TODO: remove?
        return if (amount.type == AmountType.Coin) {
            EthereumWalletManager.GasLimit.Default
        } else {
            when (amount.currencySymbol) {
                "DGX" -> EthereumWalletManager.GasLimit.High
                "AWG" -> EthereumWalletManager.GasLimit.Medium
                else -> EthereumWalletManager.GasLimit.Erc20
            }
        }
    }
}