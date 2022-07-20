package net.mm2d.preference.sample

import android.content.res.Configuration
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.mm2d.preference.Header
import net.mm2d.preference.PreferenceActivityCompat

class SettingsActivity : PreferenceActivityCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onIsMultiPane(): Boolean {
        val size = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return size >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    override fun onBuildHeaders(target: MutableList<Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    override fun isValidFragment(fragmentName: String?): Boolean {
        return (PreferenceFragmentCompat::class.java.name == fragmentName
            || GeneralPreferenceFragment::class.java.name == fragmentName
            || DataSyncPreferenceFragment::class.java.name == fragmentName
            || NotificationPreferenceFragment::class.java.name == fragmentName)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!super.onSupportNavigateUp()) {
            finish()
        }
        return true
    }

    class GeneralPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_general)
            setHasOptionsMenu(true)
            bindPreferenceSummaryToValue(findPreference("example_text")!!)
            findPreference<Preference>("notification")?.setOnPreferenceClickListener {
                (activity as SettingsActivity).startWithFragment(
                    NotificationPreferenceFragment::class.java.name,
                    null, null, 0
                )
                true
            }
        }
    }

    class NotificationPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_notification)
            setHasOptionsMenu(true)
        }
    }

    class DataSyncPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_data_sync)
            setHasOptionsMenu(true)
            bindPreferenceSummaryToValue(findPreference("sync_frequency")!!)
        }
    }

    companion object {
        private val sBindPreferenceSummaryToValueListener =
            OnPreferenceChangeListener { preference, newValue ->
                if (preference is ListPreference) {
                    val index = preference.findIndexOfValue(newValue.toString())
                    preference.summary = if (index >= 0) preference.entries[index] else null
                } else {
                    preference.summary = newValue.toString()
                }
                true
            }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
            val currentValue = PreferenceManager.getDefaultSharedPreferences(preference.context)
                .getString(preference.key, "")
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, currentValue)
        }
    }
}
