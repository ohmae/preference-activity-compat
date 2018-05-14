/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.annotation.SuppressLint;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.List;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
@SuppressLint("Registered")
public class PreferenceActivityCompat extends AppCompatActivity
        implements PreferenceActivityCompatDelegate.Connector {
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
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        mDelegate.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle state) {
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
        return getResources().getBoolean(R.bool.dual_pane);
    }

    @Override
    public void onBuildHeaders(final List<Header> target) {
    }

    @Override
    public boolean isValidFragment(final String fragmentName) {
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
            @XmlRes int resId,
            final List<Header> target) {
        mDelegate.loadHeadersFromResource(resId, target);
    }

    public void setListFooter(final View view) {
        mDelegate.setListFooter(view);
    }

    public void switchToHeader(final Header header) {
        mDelegate.switchToHeader(header);
    }
}
