package com.tangem.id.features.issuer.redux

import android.graphics.Bitmap
import com.tangem.id.common.entities.Button
import com.tangem.id.common.extensions.toQrCode
import org.rekotlin.StateType

sealed class IssuerButton(enabled: Boolean) : Button(enabled) {
    class IssueNewCredentials(enabled: Boolean = true) : IssuerButton(enabled)
}


data class IssuerState(
//    val issuerName: String? = "Ministry of Internal Affairs",
    val issuerAddress: String? = "",
    val issuerButton: IssuerButton = IssuerButton.IssueNewCredentials()
) : StateType {
    fun  getIssuerQrCode(): Bitmap? = issuerAddress?.toQrCode()
}

