package com.tangem.id.card

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.KeyPair
import com.tangem.TangemSdkError
import com.tangem.commands.CommandResponse
import com.tangem.commands.file.ReadFileDataCommand
import com.tangem.commands.file.WriteFileDataCommand
import com.tangem.commands.personalization.entities.Issuer
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.crypto.sign

class WriteFilesResponse(
    val cardId: String,
    val filesIndices: List<Int>
) : CommandResponse

class WriteFilesTask(
    private val data: List<ByteArray>,
    private val issuerKeys: KeyPair
) : CardSessionRunnable<WriteFilesResponse> {

    override val requiresPin2 = false
    private val filesIndices = mutableListOf<Int>()

    override fun run(session: CardSession, callback: (result: CompletionResult<WriteFilesResponse>) -> Unit) {
        val command = ReadFileDataCommand()
        command.run(session) { readResponse ->
            when (readResponse) {
                is CompletionResult.Failure -> callback(CompletionResult.Failure(readResponse.error))
                is CompletionResult.Success -> {
                    val counter = readResponse.data.fileDataCounter ?: 0
                    val cardId = session.environment.card?.cardId
                    if (cardId == null) {
                        callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
                        return@run
                    }
                    writeData(0, counter.inc(), cardId, session, callback)
                }
            }
        }
    }

    private fun writeData(
        currentFileIndex: Int, counter: Int,
        cardId: String, session: CardSession,
        callback: (result: CompletionResult<WriteFilesResponse>) -> Unit
    ) {
        if (currentFileIndex > data.lastIndex) {
            callback(
                CompletionResult.Success(
                    WriteFilesResponse(cardId, filesIndices)
                )
            )
            return
        }

        val currentFile = data[currentFileIndex]
        val writeCommand = WriteFileDataCommand(
            currentFile,
            getStartingSignature(currentFile, counter, cardId),
            getFinalizingSignature(currentFile, counter, cardId),
            counter, issuerKeys.publicKey
        )
        writeCommand.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    writeData(currentFileIndex.inc(), counter.inc(), cardId, session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun getStartingSignature(data: ByteArray, counter: Int, cardId: String): ByteArray {
        return (cardId.hexToBytes() + counter.toByteArray(4) + data.size.toByteArray(2))
            .sign(issuerKeys.privateKey)
    }

    private fun getFinalizingSignature(data: ByteArray, counter: Int, cardId: String): ByteArray {
        return (cardId.hexToBytes() + data + counter.toByteArray(4))
            .sign(issuerKeys.privateKey)
    }
}

fun issuer(): Issuer {
    val name = "TANGEM SDK"
    return Issuer(
        name = name,
        id = name + "\u0000",
        dataKeyPair = KeyPair(
            ("045f16bd1d2eafe463e62a335a09e6b2bbcbd04452526885cb679fc4d27af1bd22f553c7deefb54fd3d4f" +
                    "361d14e6dc3f11b7d4ea183250a60720ebdf9e110cd26").hexToBytes(),
            "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()
        ),
        transactionKeyPair = KeyPair(
            ("0484c5192e9bfa6c528a344f442137a92b89ea835bfef1d04cb4362eb906b508c5889846cfea71ba6dc7b" +
                    "3120c2208df9c46127d3d85cb5cfbd1479e97133a39d8").hexToBytes(),
            "11121314151617184771ED81F2BACF57479E4735EB1405081918171615141312".hexToBytes()
        )
    )
}