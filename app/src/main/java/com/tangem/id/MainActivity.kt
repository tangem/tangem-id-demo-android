package com.tangem.id

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tangem.id.features.starter.MainFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.fragment_container,
                MainFragment()
            )
            .commit();
    }
}