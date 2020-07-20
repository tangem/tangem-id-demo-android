package com.tangem.id.features.issuecredentials.redux

import com.tangem.id.common.redux.*
import org.rekotlin.StateType

sealed class NewCredentialsButton(enabled: Boolean) : Button(enabled) {
    class Sign(enabled: Boolean = true) : NewCredentialsButton(enabled)
    class WriteCredentials(enabled: Boolean = true) : NewCredentialsButton(enabled)
}

data class IssueCredentialsState(
    val editable: Boolean = true,
    val photo: Photo? = Photo(),
    val passport: Passport? = Passport(),
    val securityNumber: SecurityNumber? = SecurityNumber(),
    val ageOfMajority: AgeOfMajority? = AgeOfMajority(),
    val button: NewCredentialsButton = NewCredentialsButton.Sign()
) : StateType {
    val credentials = listOfNotNull(photo, passport, securityNumber, ageOfMajority)
    val inputDataReady =
        !credentials.map { it.isDataPresent() }.contains(false)
}