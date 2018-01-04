package com.balicodes.quicksms

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

/**
 * Created by eka on 6/28/15.
 */

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        fragmentManager
                .beginTransaction()
                .replace(R.id.preference_container, SettingsFragment())
                .commit()
    }
}