package com.tangem.id.features.holder.redux

import com.tangem.id.common.redux.*
import org.rekotlin.StateType

enum class AccessLevel {
    Private, Public;

    fun toggleVisibility(): AccessLevel {
        return when (this) {
            Private -> Public
            Public -> Private
        }
    }
}

data class CredentialsAccessLevels(
    val photo: AccessLevel = AccessLevel.Public,
    val passport: AccessLevel = AccessLevel.Public,
    val securityNumber: AccessLevel = AccessLevel.Public,
    val ageOfMajority: AccessLevel = AccessLevel.Public,
    val immunity: AccessLevel = AccessLevel.Public
) {
    fun toggleAccessLevel(credential: Credential): CredentialsAccessLevels {
        return when (credential) {
            is Photo -> this.copy(photo = photo.toggleVisibility())
            is Passport -> this.copy(passport = passport.toggleVisibility())
            is SecurityNumber -> this.copy(securityNumber = securityNumber.toggleVisibility())
            is AgeOfMajority -> this.copy(ageOfMajority = ageOfMajority.toggleVisibility())
            is ImmunityPassport -> this.copy(photo = photo.toggleVisibility())
            else -> this
        }
    }

    fun getAccessLevel(credential: Credential): AccessLevel {
        return when (credential) {
            is Photo -> photo
            is Passport -> passport
            is SecurityNumber -> securityNumber
            is AgeOfMajority -> ageOfMajority
            is ImmunityPassport -> immunity
            else -> AccessLevel.Public
        }
    }
}

sealed class HolderScreenButton(enabled: Boolean) : Button(enabled) {
    class RequestNewCredential(enabled: Boolean = true) : HolderScreenButton(enabled)
    class SaveChanges(enabled: Boolean = true) : HolderScreenButton(enabled)
}

data class HolderState(
    val cardId: String? = null,
    val editActivated: Boolean = false,
    val detailsOpened: Credential? = null,
    val credentialsToDelete: List<Credential> = listOf(),
    val photo: Photo? = Photo(),
    val passport: Passport? = Passport(),
    val securityNumber: SecurityNumber? = SecurityNumber("000-00-000"),
    val ageOfMajority: AgeOfMajority? = AgeOfMajority(true),
    val immunityPassport: ImmunityPassport? = null,
    val accessLevelsFromCard: CredentialsAccessLevels = CredentialsAccessLevels(),
    val accessLevelsModified: CredentialsAccessLevels = accessLevelsFromCard
) : StateType {

    val button: HolderScreenButton = if (editActivated) {
        HolderScreenButton.SaveChanges()
    } else {
        HolderScreenButton.RequestNewCredential()
    }

    val credentials =
        listOfNotNull(photo, passport, securityNumber, ageOfMajority, immunityPassport)
}

