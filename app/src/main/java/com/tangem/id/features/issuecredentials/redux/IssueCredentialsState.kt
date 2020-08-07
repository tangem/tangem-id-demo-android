package com.tangem.id.features.issuecredentials.redux

import com.tangem.Log
import com.tangem.id.common.redux.*
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IssueCredentialsState

        if (editable != other.editable) return false
        if (jsonShown != other.jsonShown) return false
//        if (holdersAddress != other.holdersAddress) return false
        if (photo != other.photo) return false
        if (passport != other.passport) return false
        if (securityNumber != other.securityNumber) return false
        if (ageOfMajority != other.ageOfMajority) return false
        if (button != other.button) return false
        if (issueCredentialsCompleted != other.issueCredentialsCompleted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = editable.hashCode()
//        result = 31 * result + (holdersAddress?.hashCode() ?: 0)
        result = 31 * result + (photo?.hashCode() ?: 0)
        result = 31 * result + (passport?.hashCode() ?: 0)
        result = 31 * result + (securityNumber?.hashCode() ?: 0)
        result = 31 * result + (ageOfMajority?.hashCode() ?: 0)
        result = 31 * result + button.hashCode()
        result = 31 * result + issueCredentialsCompleted.hashCode()
        result = 31 * result + (jsonShown?.hashCode() ?: 0)
        return result
    }


}