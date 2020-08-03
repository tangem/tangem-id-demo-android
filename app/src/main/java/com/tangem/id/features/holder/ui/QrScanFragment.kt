package com.tangem.id.features.holder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.zxing.Result
import com.tangem.id.common.redux.navigation.AppScreen
import com.tangem.id.common.redux.navigation.NavigationAction
import com.tangem.id.store
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScanFragment : Fragment(0), ZXingScannerView.ResultHandler {

    private var scannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        scannerView = ZXingScannerView(activity)
        return scannerView
    }

    override fun onPause() {
        super.onPause()
        scannerView?.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        scannerView?.setResultHandler(this)
        scannerView?.startCamera()
    }

    override fun handleResult(result: Result) {
//      result.text
//        store.dispatch(HolderAction.RequestNewCredential.Success(ImmunityPassport(true)))
        store.dispatch(
            NavigationAction.PopBackTo(AppScreen.Holder)
        )
    }

}