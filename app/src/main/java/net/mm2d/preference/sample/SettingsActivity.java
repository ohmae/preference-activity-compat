package net.mm2d.preference.sample;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import net.mm2d.preference.Header;
import net.mm2d.preference.PreferenceActivityCompat;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView.Adapter;

public class SettingsActivity extends PreferenceActivityCompat {
    private static final OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);
        } else {
            preference.setSummary(stringValue);
        }
        return true;
    };

    private static boolean isXLargeTablet(final Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private static void bindPreferenceSummaryToValue(final Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    public void onBuildHeaders(@NonNull final List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public boolean isValidFragment(final String fragmentName) {
        return PreferenceFragmentCompat.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class GeneralPreferenceFragment extends CustomPreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(
                final Bundle savedInstanceState,
                final String rootKey) {

            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends CustomPreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(
                final Bundle savedInstanceState,
                final String rootKey) {

            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends CustomPreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(
                final Bundle savedInstanceState,
                final String rootKey) {
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }
    }

    private static abstract class CustomPreferenceFragmentCompat extends PreferenceFragmentCompat {
        @Override
        protected Adapter onCreateAdapter(final PreferenceScreen preferenceScreen) {
            return new CustomPreferenceGroupAdapter(preferenceScreen);
        }
    }

    @SuppressLint("RestrictedApi")
    private static class CustomPreferenceGroupAdapter extends PreferenceGroupAdapter {
        CustomPreferenceGroupAdapter(final PreferenceGroup preferenceGroup) {
            super(preferenceGroup);
        }

        @NonNull
        @Override
        public PreferenceViewHolder onCreateViewHolder(
                final ViewGroup parent,
                final int viewType) {
            final PreferenceViewHolder holder = super.onCreateViewHolder(parent, viewType);
            final View icon = holder.findViewById(android.R.id.icon);
            if (icon != null) {
                final ViewParent p = icon.getParent();
                if (p instanceof View) {
                    final View parentView = (View) p;
                    parentView.setMinimumWidth(0);
                    parentView.setPadding(0,0,0,0);
                }
            }
            return holder;
        }
    }
}
