package com.tangem.id.features.issuecredentials.ui.widgets

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.id.R
import com.tangem.id.common.extensions.hide
import com.tangem.id.common.extensions.show
import com.tangem.id.common.redux.Photo
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.layout_photo_editable.*

class EditablePhotoWidget(private val fragment: Fragment) :
    CredentialWidget<Photo>(fragment.context) {
    override val viewToInflate = R.layout.layout_photo_editable
    override val rootView: ViewGroup? = fragment.fl_photo_root

    override fun setup(credential: Photo, editable: Boolean) {
        if (credential.photo != null) {
            fragment.btn_add_photo?.hide()
            fragment.view_photo?.show()
            fragment.iv_photo?.show()
            fragment.iv_photo?.setImageBitmap(credential.photo)

        } else {
            fragment.view_photo?.hide()
            fragment.iv_photo?.hide()
            fragment.btn_add_photo?.show()
            fragment.btn_add_photo?.setOnClickListener {
                store.dispatch(
                    NavigationAction.NavigateTo(AppScreen.Camera, fragment.requireActivity())
                )
            }
        }
    }
}