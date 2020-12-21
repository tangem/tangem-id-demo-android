package com.tangem.id

import android.content.Context
import com.example.tangem_id_core.R
import com.tangem.TangemError

sealed class TangemIdError(context: Context) : TangemError, Throwable() {

    class WrongIssuerCardType(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_wrong_issuer_card_type
        override var customMessage = context.getString(messageResId)
    }

    class WrongHolderCardType(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_wrong_holder_card_type
        override var customMessage = context.getString(messageResId)
    }

    class ReadingCardError(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_reading_card
        override var customMessage = context.getString(messageResId)
    }

    class ErrorAddingNewCredential(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_adding_new_credential
        override var customMessage = context.getString(messageResId)
    }

    class ErrorWritingCredentials(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_writing_credentials
        override var customMessage = context.getString(messageResId)
    }

    class ErrorCreatingCredentials(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_creating_credentials
        override var customMessage = context.getString(messageResId)
    }

    class CardError(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_card_error
        override var customMessage = context.getString(messageResId)
    }

    class ConvertingCredentialError(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_converting_credential
        override var customMessage = context.getString(messageResId)
    }

    class CredentialAlreadyIssued(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_credential_already_issued
        override var customMessage = context.getString(messageResId)
    }

    class NoCredentials(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_no_credentials
        override var customMessage = context.getString(messageResId)
    }

    class NoVisibleCredentials(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_no_visible_credentials
        override var customMessage = context.getString(messageResId)
    }

    class UserCancelled(context: Context) : TangemIdError(context) {
        override val code = 0
        override val messageResId: Int = R.string.error_user_cancelled
        override var customMessage = context.getString(messageResId)
    }




}