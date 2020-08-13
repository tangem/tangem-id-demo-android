package com.tangem.id.features.issuecredentials.redux

import com.tangem.Log
import com.tangem.id.common.entities.*
import org.rekotlin.StateType

sealed class IssueCredentialsButton(enabled: Boolean) : Button(enabled) {
    class Sign(enabled: Boolean = true, val progress: Boolean = false) : IssueCredentialsButton(enabled)
    class WriteCredentials(enabled: Boolean = true) : IssueCredentialsButton(enabled)
}

data class IssueCredentialsState(
    val editable: Boolean = true,
    val jsonShown: String? = null,
    val holdersAddress: String? = null,
    val photo: Photo? = Photo(),
    val passport: Passport? = Passport(),
    val securityNumber: SecurityNumber? = SecurityNumber(),
    val ageOfMajority: AgeOfMajority? = AgeOfMajority(),
    val button: IssueCredentialsButton = IssueCredentialsButton.Sign(),
    val issueCredentialsCompleted: Boolean = false
) : StateType {

    init {
        Log.v("TangemIssState", this.toString())
    }

    fun getCredentials() = listOfNotNull(photo, passport, securityNumber, ageOfMajority)
    fun isInputDataReady() =
        !getCredentials().map { it.isDataPresent() }.contains(false)

    fun isInputDataModified(): Boolean {
        if (!editable) return true

        return photo?.isDataPresent() == true || passport?.isInputDataModified() == true
                || securityNumber?.number != null
    }
}