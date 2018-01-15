/*
 * Copyright (C) 2016-2018 Eka Putra
 *
 * Quick SMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.balicodes.quicksms

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

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
