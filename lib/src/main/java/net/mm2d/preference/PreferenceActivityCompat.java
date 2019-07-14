/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
@SuppressLint("Registered")
public class PreferenceActivityCompat extends AppCompatActivity implements
        PreferenceActivityCompatDelegate.Connector,
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":android:show_fragment_args";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":android:show_fragment_title";
    public static final String EXTRA_SHOW_FRAGMENT_SHORT_TITLE
            = ":android:show_fragment_short_title";
    public static final String EXTRA_NO_HEADERS = ":android:no_headers";

    private PreferenceActivityCompatDelegate mDelegate;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDelegate = new PreferenceActivityCompatDelegate(this, this);
        mDelegate.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        mDelegate.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        mDelegate.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle state) {
        super.onRestoreInstanceState(state);
        mDelegate.onRestoreInstanceState(state);
    }

    @Override
    public void onBackPressed() {
        if (mDelegate.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onIsMultiPane() {
        return getResources().getBoolean(R.bool.mm2d_pac_dual_pane);
    }

    @Override
    public boolean onIsHidingHeaders() {
        return mDelegate.onIsHidingHeaders();
    }

    @Override
    public void onBuildHeaders(@NonNull final List<Header> target) {
    }

    @Override
    public boolean isValidFragment(@Nullable final String fragmentName) {
        if (getApplicationInfo().targetSdkVersion >= VERSION_CODES.KITKAT) {
            throw new RuntimeException(
                    "Subclasses of PreferenceActivity must override isValidFragment(String)"
                            + " to verify that the Fragment class is valid! "
                            + getClass().getName()
                            + " has not checked if fragment " + fragmentName + " is valid.");
        } else {
            return true;
        }
    }

    public int getSelectedItemPosition() {
        return mDelegate.getSelectedItemPosition();
    }

    public boolean hasHeaders() {
        return mDelegate.hasHeaders();
    }

    @NonNull
    public List<Header> getHeaders() {
        return mDelegate.getHeaders();
    }

    public boolean isMultiPane() {
        return mDelegate.isMultiPane();
    }

    public void invalidateHeaders() {
        mDelegate.invalidateHeaders();
    }

    public void loadHeadersFromResource(
            @XmlRes final int resId,
            @NonNull final List<Header> target) {
        mDelegate.loadHeadersFromResource(resId, target);
    }

    public void setListFooter(@NonNull final View view) {
        mDelegate.setListFooter(view);
    }

    public void switchToHeader(
            @NonNull final String fragmentName,
            @Nullable final Bundle args) {
        mDelegate.switchToHeader(fragmentName, args);
    }

    public void switchToHeader(@NonNull final Header header) {
        mDelegate.switchToHeader(header);
    }

    @Override
    public boolean onPreferenceStartFragment(
            @NonNull final PreferenceFragmentCompat caller,
            @NonNull final Preference pref) {
        mDelegate.startPreferenceFragment(pref);
        return true;
    }

    public Intent onBuildStartFragmentIntent(
            @NonNull final String fragmentName,
            @Nullable final Bundle args,
            @StringRes final int titleRes,
            @StringRes final int shortTitleRes
    ) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(this, getClass());
        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, titleRes);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, shortTitleRes);
        intent.putExtra(EXTRA_NO_HEADERS, true);
        return intent;
    }

    public void startWithFragment(
            @NonNull final String fragmentName,
            @Nullable final Bundle args,
            @Nullable final Fragment resultTo,
            final int resultRequestCode
    ) {
        startWithFragment(fragmentName, args, resultTo, resultRequestCode, 0, 0);
    }

    public void startWithFragment(
            @NonNull final String fragmentName,
            @Nullable final Bundle args,
            @Nullable final Fragment resultTo,
            final int resultRequestCode,
            @StringRes final int titleRes,
            @StringRes final int shortTitleRes) {
        Intent intent = onBuildStartFragmentIntent(fragmentName, args, titleRes, shortTitleRes);
        if (resultTo == null) {
            startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }
}
