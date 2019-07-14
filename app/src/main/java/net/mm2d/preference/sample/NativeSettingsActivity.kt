package net.mm2d.preference.sample

import android.content.res.Configuration
import android.os.Bundle
import android.preference.*
import android.preference.Preference.OnPreferenceChangeListener
import android.view.MenuItem

class NativeSettingsActivity : AppCompatPreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onIsMultiPane(): Boolean {
        val size = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return size >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.native_pref_headers, target)
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return (PreferenceFragment::class.java.name == fragmentName
                || GeneralPreferenceFragment::class.java.name == fragmentName
                || DataSyncPreferenceFragment::class.java.name == fragmentName
                || NotificationPreferenceFragment::class.java.name == fragmentName)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.native_pref_general)
            setHasOptionsMenu(true)
            bindPreferenceSummaryToValue(findPreference("example_text"))
            findPreference("notification")?.setOnPreferenceClickListener {
                (activity as NativeSettingsActivity).startWithFragment(
                    NotificationPreferenceFragment::class.java.name,
                    null, null, 0
                )
                true
            }
        }
    }

    class NotificationPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.native_pref_notification)
            setHasOptionsMenu(true)
        }
    }

    class DataSyncPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.native_pref_data_sync)
            setHasOptionsMenu(true)
            bindPreferenceSummaryToValue(findPreference("sync_frequency"))
        }
    }

    companion object {
        private val sBindPreferenceSummaryToValueListener = object : OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                if (preference is ListPreference) {
                    val index = preference.findIndexOfValue(newValue.toString())
                    preference.summary = if (index >= 0) preference.entries[index] else null
                } else {
                    preference.summary = newValue.toString()
                }
                return true
            }
        }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
            val currentValue = PreferenceManager.getDefaultSharedPreferences(preference.context)
                .getString(preference.key, "")
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, currentValue)
        }
    }
}
