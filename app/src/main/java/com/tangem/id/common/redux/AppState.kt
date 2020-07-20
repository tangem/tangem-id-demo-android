package com.tangem.id.common.redux

import android.graphics.Bitmap
import com.tangem.id.common.redux.navigation.NavigationState
import com.tangem.id.features.holder.redux.HolderState
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsState
import com.tangem.id.features.issuer.redux.IssuerState
import com.tangem.id.features.verifier.redux.VerifierState
import org.rekotlin.StateType

data class AppState(
    val navigationState: NavigationState = NavigationState(),
    val issuerState: IssuerState = IssuerState(),
    val issueCredentialsState: IssueCredentialsState = IssueCredentialsState(),
    val holderState: HolderState = HolderState(),
    val verifierState: VerifierState = VerifierState()
) : StateType

abstract class Button(val enabled: Boolean)

interface Credential {
    fun isDataPresent(): Boolean
}

data class Photo(val photo: Bitmap? = null) : Credential {
    override fun isDataPresent(): Boolean = photo != null
}

data class Passport(
    val name: String? = null,
    val surname: String? = null,
    val gender: Gender? = null,
    val birthDate: String? = null
) : Credential {
    override fun isDataPresent() =
        name != null && surname != null && gender != null && birthDate != null
}

data class SecurityNumber(val number: String? = null) : Credential {
    override fun isDataPresent(): Boolean = number != null
}

data class AgeOfMajority(val valid: Boolean? = null) : Credential {
    override fun isDataPresent(): Boolean = valid != null
}

data class ImmunityPassport(val validUntil: String? = null) : Credential {
    override fun isDataPresent(): Boolean = validUntil != null
}

enum class Gender {
    Male, Female, Other;

    companion object {
        private val values = values()
        fun byOrdinal(ordinal: Int): Gender = values[ordinal]
    }
}