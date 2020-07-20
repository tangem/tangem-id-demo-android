package com.tangem.id.features.issuecredentials.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import com.tangem.id.R
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.store
import kotlinx.android.synthetic.main.fragment_camera.*

class CameraFragment : Fragment(R.layout.fragment_camera) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo(activity = requireActivity()))
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cvCamera?.setLifecycleOwner(viewLifecycleOwner)

        cvCamera?.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) { // Picture was taken!
                result.toBitmap(200, 200) { bitmap ->
                    if (bitmap != null) {
                        store.dispatch(IssueCredentialsAction.AddPhoto.Success(bitmap))
                        store.dispatch(
                            NavigationAction.PopBackTo(AppScreen.IssueCredential, requireActivity())
                        )
                    } else {
                        store.dispatch(IssueCredentialsAction.AddPhoto.Failure)
                    }
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
        })
        ivTakePicture?.setOnClickListener { cvCamera?.takePictureSnapshot() }
        ivFlipCamera?.setOnClickListener { flipCamera() }
    }

    private fun flipCamera() {
        if (cvCamera?.facing == Facing.BACK) {
            cvCamera?.facing = Facing.FRONT
        } else {
            cvCamera?.facing = Facing.BACK
        }
    }

    companion object {
        const val PHOTO_KEY = "photo"
    }

}