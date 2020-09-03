package com.tangem.id.common.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment

class CameraPermissionManager {

    companion object {
        fun isPermissionGranted(fragment: Fragment): Boolean {
            return ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) ==
                    PermissionChecker.PERMISSION_GRANTED
        }

        fun handleRequestPermissionResult(
            requestCode: Int, grantResults: IntArray,
            actionIfNotGranted: () -> Unit, actionIfGranted: () -> Unit
        ) {
            when (requestCode) {
                1 -> {
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        actionIfNotGranted()
                    } else {
                        actionIfGranted()
                    }
                }
            }
        }

        fun requirePermission(fragment: Fragment) {
            fragment.requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }
    }




}