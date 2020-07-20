package com.tangem.id.features.issuer.redux

import android.graphics.Bitmap
import com.tangem.id.common.extensions.toQrCode
import com.tangem.id.common.redux.Button
import org.rekotlin.StateType

sealed class IssuerButton(enabled: Boolean) : Button(enabled) {
    class IssueNewCredentials(enabled: Boolean = true) : IssuerButton(enabled)
}


data class IssuerState(
    val issuerName: String? = "Ministry of Internal Affairs",
    val issuerAddress: String? = "did:ethr:0x91901762C7d20d2894396c189d74483aFa118f4",
    val issuerButton: IssuerButton = IssuerButton.IssueNewCredentials()
) : StateType {
    fun  getIssuerQrCode(): Bitmap? = issuerAddress?.toQrCode()
}

