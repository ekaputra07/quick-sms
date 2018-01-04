package com.balicodes.quicksms

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created by eka on 6/28/15.
 */
class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var sp: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.settings)
        sp = preferenceScreen.sharedPreferences

        // set settings summary
        setSortByPrefSummary()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Log.d("====> SettingsFragment", "onSharedPreferenceChanged: " + key)

        // Sort order changes
        if (key == getString(R.string.pref_sort_by_key))
            setSortByPrefSummary()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun setSortByPrefSummary() {
        val key = getString(R.string.pref_sort_by_key)

        var summary = ""
        val names = resources.getStringArray(R.array.pref_sort_order_names_array)
        val values = resources.getStringArray(R.array.pref_sort_order_values_array)

        val value = sp!!.getString(key, getString(R.string.pref_sort_by_default_value))
        val index = java.util.Arrays.asList(*values).indexOf(value)

        if (index != -1)
            summary = names[index]

        val sortOrderPref = findPreference(key)
        sortOrderPref.summary = summary
    }
}
