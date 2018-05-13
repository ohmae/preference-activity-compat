/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.List;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
@SuppressLint("Registered")
public class PreferenceActivityCompat extends AppCompatActivity
        implements PreferenceActivityCompatDelegate.Connector {
    private PreferenceActivityCompatDelegate mDelegate;
    public static final String EXTRA_SHOW_FRAGMENT = PreferenceActivityCompatDelegate.EXTRA_SHOW_FRAGMENT;
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = PreferenceActivityCompatDelegate.EXTRA_SHOW_FRAGMENT_ARGUMENTS;
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = PreferenceActivityCompatDelegate.EXTRA_SHOW_FRAGMENT_TITLE;
    public static final String EXTRA_SHOW_FRAGMENT_SHORT_TITLE = PreferenceActivityCompatDelegate.EXTRA_SHOW_FRAGMENT_SHORT_TITLE;
    public static final String EXTRA_NO_HEADERS = PreferenceActivityCompatDelegate.EXTRA_NO_HEADERS;

    public void setListAdapter(final ListAdapter adapter) {
        mDelegate.setListAdapter(adapter);
    }

    public void setSelection(final int position) {
        mDelegate.setSelection(position);
    }

    public int getSelectedItemPosition() {
        return mDelegate.getSelectedItemPosition();
    }

    public ListView getListView() {
        return mDelegate.getListView();
    }

    public ListAdapter getListAdapter() {
        return mDelegate.getListAdapter();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDelegate = new PreferenceActivityCompatDelegate(this, this);
        mDelegate.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (mDelegate.onBackPressed()) {
            return;
        }
        super.onBackPressed();
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

    public boolean onIsMultiPane() {
        return mDelegate.onIsMultiPane();
    }

    public boolean onIsHidingHeaders() {
        return mDelegate.onIsHidingHeaders();
    }

    public Header onGetInitialHeader() {
        return mDelegate.onGetInitialHeader();
    }

    public Header onGetNewHeader() {
        return mDelegate.onGetNewHeader();
    }

    @Override
    public void onBuildHeaders(final List<Header> target) {
    }

    public void invalidateHeaders() {
        mDelegate.invalidateHeaders();
    }

    public void loadHeadersFromResource(
            @XmlRes int resId,
            final List<Header> target) {
        mDelegate.loadHeadersFromResource(resId, target);
    }

    @Override
    public boolean isValidFragment(final String fragmentName) {
        if (getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.KITKAT) {
            throw new RuntimeException(
                    "Subclasses of PreferenceActivity must override isValidFragment(String)"
                            + " to verify that the Fragment class is valid! "
                            + getClass().getName()
                            + " has not checked if fragment " + fragmentName + " is valid.");
        } else {
            return true;
        }
    }

    public void setListFooter(final View view) {
        mDelegate.setListFooter(view);
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

    public void showBreadCrumbs(
            final CharSequence title,
            final CharSequence shortTitle) {
        mDelegate.showBreadCrumbs(title, shortTitle);
    }

    public void switchToHeader(
            final String fragmentName,
            final Bundle args) {
        mDelegate.switchToHeader(fragmentName, args);
    }

    public void switchToHeader(final Header header) {
        mDelegate.switchToHeader(header);
    }
}
