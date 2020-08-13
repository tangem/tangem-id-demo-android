package com.tangem.id.common.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.tangem.id.features.issuecredentials.redux.IssueCredentialsAction
import com.tangem.id.store

class CameraPermissionManager {

    companion object {
        fun isPermissionGranted(fragment: Fragment): Boolean {
            return ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) ==
                    PermissionChecker.PERMISSION_GRANTED
        }

        fun handleRequestPermissionResult(requestCode: Int, grantResults: IntArray, action: () -> Unit) {
            when (requestCode) {
                1 -> {
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        store.dispatch(IssueCredentialsAction.NoCameraPermission)
                    } else {
                        action.invoke()
                    }
                }
            }
        }

        fun requirePermission(fragment: Fragment) {
            fragment.requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }
    }




}