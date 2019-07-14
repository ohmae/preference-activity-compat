/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle.State;
import androidx.preference.Preference;

import static net.mm2d.preference.PreferenceActivityCompat.*;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class PreferenceActivityCompatDelegate {
    public interface Connector {
        void onBuildHeaders(@NonNull List<Header> target);

        boolean onIsMultiPane();

        boolean onIsHidingHeaders();

        boolean isValidFragment(@Nullable String fragmentName);
    }

    static final long HEADER_ID_UNDEFINED = -1;

    private static final String HEADERS_TAG = ":android:headers";
    private static final String CUR_HEADER_TAG = ":android:cur_header";
    private static final String BACK_STACK_PREFS = ":android:prefs";

    @NonNull
    private final FragmentActivity mActivity;
    @NonNull
    private final Connector mConnector;
    @NonNull
    private final OnItemClickListener mOnClickListener =
            (parent, view, position, id) -> onListItemClick(position);
    @NonNull
    private final ArrayList<Header> mHeaders = new ArrayList<>();
    private ListAdapter mAdapter;
    private ListView mList;
    private boolean mFinishedStart = false;
    private FrameLayout mListFooter;
    private ViewGroup mPrefsContainer;
    private ViewGroup mHeadersContainer;
    private TextView mBreadCrumbTitle;
    private boolean mSinglePane;
    private Header mCurrentHeader;
    private final Handler mHandler = new Handler();
    private Fragment mFragment;

    private final Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    private final Runnable mBuildHeaders = new Runnable() {
        @Override
        public void run() {
            mHeaders.clear();
            mConnector.onBuildHeaders(mHeaders);
            if (mAdapter instanceof BaseAdapter) {
                ((BaseAdapter) mAdapter).notifyDataSetChanged();
            }
            if (mCurrentHeader != null) {
                final Header mappedHeader = findBestMatchingHeader(mCurrentHeader, mHeaders);
                if (mappedHeader != null) {
                    setSelectedHeader(mappedHeader);
                }
            }
        }
    };

    PreferenceActivityCompatDelegate(
            @NonNull final FragmentActivity activity,
            @NonNull final Connector connector) {
        mActivity = activity;
        mConnector = connector;
    }

    @NonNull
    private Context getContext() {
        return mActivity;
    }

    @NonNull
    private Intent getIntent() {
        return mActivity.getIntent();
    }

    @NonNull
    private Resources getResources() {
        return mActivity.getResources();
    }

    private boolean isResumed() {
        return mActivity.getLifecycle().getCurrentState() == State.RESUMED;
    }

    @NonNull
    private FragmentManager getFragmentManager() {
        return mActivity.getSupportFragmentManager();
    }

    @Nullable
    private CharSequence getTitle() {
        return mActivity.getTitle();
    }

    private void setTitle(@Nullable final CharSequence title) {
        mActivity.setTitle(title);
    }

    private void setContentView(@LayoutRes final int layoutResID) {
        mActivity.setContentView(layoutResID);
    }

    @Nullable
    private <T extends View> T findViewById(@IdRes final int id) {
        return mActivity.findViewById(id);
    }

    void onCreate(@Nullable final Bundle savedInstanceState) {
        setContentView(R.layout.mm2d_pac_content);
        mList = findViewById(R.id.list);
        mList.setOnItemClickListener(mOnClickListener);
        if (mFinishedStart) {
            setListAdapter(mAdapter);
        }
        mHandler.post(mRequestFocus);
        mFinishedStart = true;
        mListFooter = findViewById(R.id.list_footer);
        mPrefsContainer = findViewById(R.id.prefs_frame);
        mHeadersContainer = findViewById(R.id.headers);

        mSinglePane = mConnector.onIsHidingHeaders() || !mConnector.onIsMultiPane();
        final View breadCrumbSection = findViewById(R.id.breadcrumb_section);
        mBreadCrumbTitle = findViewById(R.id.bread_crumb_title);
        if (mSinglePane && breadCrumbSection != null && mBreadCrumbTitle != null) {
            mBreadCrumbTitle.setVisibility(View.GONE);
            breadCrumbSection.setVisibility(View.GONE);
        }

        final String initialFragment = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        final Bundle initialArguments = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);

        if (savedInstanceState != null) {
            final ArrayList<Header> headers = savedInstanceState.getParcelableArrayList(HEADERS_TAG);
            if (headers != null) {
                mHeaders.addAll(headers);
                final int curHeader = savedInstanceState.getInt(CUR_HEADER_TAG,
                        (int) HEADER_ID_UNDEFINED);
                if (curHeader >= 0 && curHeader < mHeaders.size()) {
                    setSelectedHeader(mHeaders.get(curHeader));
                } else if (!mSinglePane) {
                    switchToHeader(onGetInitialHeader());
                }
            } else {
                showBreadCrumbs(getTitle());
            }
        } else {
            if (initialFragment == null || !mConnector.onIsHidingHeaders()) {
                mConnector.onBuildHeaders(mHeaders);
            }
            if (initialFragment != null) {
                switchToHeader(initialFragment, initialArguments);
            } else if (!mSinglePane && mHeaders.size() > 0) {
                switchToHeader(onGetInitialHeader());
            }
        }
        if (mHeaders.size() > 0) {
            setListAdapter(new HeaderAdapter(getContext(), mHeaders));
            if (!mSinglePane) {
                mList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            }
        }

        int initialTitle = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
        if (mSinglePane && initialFragment != null && initialTitle != 0) {
            showBreadCrumbs(mActivity.getText(initialTitle));
        }

        if (mSinglePane) {
            if (mCurrentHeader != null || initialFragment != null) {
                mHeadersContainer.setVisibility(View.GONE);
            } else {
                mPrefsContainer.setVisibility(View.GONE);
            }
        } else if (mHeaders.size() > 0 && mCurrentHeader != null) {
            setSelectedHeader(mCurrentHeader);
        }
    }

    void onDestroy() {
        mHandler.removeCallbacks(mBuildHeaders);
        mHandler.removeCallbacks(mRequestFocus);
    }

    void onSaveInstanceState(@NonNull final Bundle outState) {
        if (mHeaders.size() > 0) {
            outState.putParcelableArrayList(HEADERS_TAG, mHeaders);
            if (mCurrentHeader != null) {
                final int index = mHeaders.indexOf(mCurrentHeader);
                if (index >= 0) {
                    outState.putInt(CUR_HEADER_TAG, index);
                }
            }
        }
    }

    void onRestoreInstanceState(@NonNull final Bundle state) {
        if (!mSinglePane) {
            if (mCurrentHeader != null) {
                setSelectedHeader(mCurrentHeader);
            }
        }
    }

    boolean onBackPressed() {
        final FragmentManager manager = getFragmentManager();
        if (mCurrentHeader == null || !mSinglePane || manager.getBackStackEntryCount() != 0) {
            return false;
        }
        if (mFragment != null) {
            manager.beginTransaction()
                    .remove(mFragment)
                    .commitAllowingStateLoss();
            mFragment = null;
        }
        mCurrentHeader = null;
        mPrefsContainer.setVisibility(View.GONE);
        mHeadersContainer.setVisibility(View.VISIBLE);
        showBreadCrumbs(getTitle());
        mList.clearChoices();
        return true;
    }

    private void setListAdapter(final ListAdapter adapter) {
        mAdapter = adapter;
        mList.setAdapter(adapter);
    }

    int getSelectedItemPosition() {
        return mList.getSelectedItemPosition();
    }

    boolean hasHeaders() {
        return mHeadersContainer != null && mHeadersContainer.getVisibility() == View.VISIBLE;
    }

    @NonNull
    List<Header> getHeaders() {
        return mHeaders;
    }

    boolean onIsHidingHeaders() {
        return getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false);
    }

    boolean isMultiPane() {
        return !mSinglePane;
    }

    @NonNull
    private Header onGetInitialHeader() {
        for (int i = 0; i < mHeaders.size(); i++) {
            final Header h = mHeaders.get(i);
            if (h.fragment != null) {
                return h;
            }
        }
        throw new IllegalStateException("Must have at least one header with a fragment");
    }

    void invalidateHeaders() {
        mHandler.removeCallbacks(mBuildHeaders);
        mHandler.post(mBuildHeaders);
    }

    void loadHeadersFromResource(
            @XmlRes final int resId,
            @NonNull final List<Header> target) {
        HeaderLoader.loadFromResource(getContext(), resId, target);
    }

    void setListFooter(@NonNull final View view) {
        mListFooter.removeAllViews();
        mListFooter.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    private void onListItemClick(final int position) {
        if (!isResumed()) {
            return;
        }
        if (mAdapter != null) {
            final Object item = mAdapter.getItem(position);
            if (item instanceof Header) onHeaderClick((Header) item);
        }
    }

    private void onHeaderClick(@NonNull final Header header) {
        if (header.fragment != null) {
            switchToHeader(header);
        } else if (header.intent != null) {
            getContext().startActivity(header.intent);
        }
    }

    void switchToHeader(
            @NonNull final String fragmentName,
            @Nullable final Bundle args) {
        Header selectedHeader = null;
        for (final Header header : mHeaders) {
            if (fragmentName.equals(header.fragment)) {
                selectedHeader = header;
                break;
            }
        }
        if (selectedHeader != null) {
            setSelectedHeader(selectedHeader);
        }
        switchToHeaderInner(fragmentName, args);
    }

    void switchToHeader(@NonNull final Header header) {
        if (mCurrentHeader == header) {
            getFragmentManager().popBackStack(BACK_STACK_PREFS, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            if (header.fragment == null) {
                throw new IllegalStateException("can't switch to header that has no fragment");
            }
            mHandler.post(() -> {
                switchToHeaderInner(header.fragment, header.fragmentArguments);
                setSelectedHeader(header);
            });
        }
    }

    private void switchToHeaderInner(
            @NonNull final String fragmentName,
            @Nullable final Bundle args) {
        final FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.popBackStack(BACK_STACK_PREFS, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (!mConnector.isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: " + fragmentName);
        }
        mFragment = Fragment.instantiate(getContext(), fragmentName, args);
        fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .replace(R.id.prefs, mFragment)
                .commitAllowingStateLoss();

        if (mSinglePane && mPrefsContainer.getVisibility() == View.GONE) {
            mPrefsContainer.setVisibility(View.VISIBLE);
            mHeadersContainer.setVisibility(View.GONE);
        }
    }

    private void setSelectedHeader(@NonNull final Header header) {
        mCurrentHeader = header;
        final int index = mHeaders.indexOf(header);
        if (index >= 0) {
            mList.setItemChecked(index, true);
        } else {
            mList.clearChoices();
        }
        showBreadCrumbs(header);
    }

    private void showBreadCrumbs(@NonNull final Header header) {
        final Resources resources = getResources();
        CharSequence title = header.getBreadCrumbTitle(resources);
        if (title == null) title = header.getTitle(resources);
        if (title == null) title = getTitle();
        showBreadCrumbs(title);
    }

    private void showBreadCrumbs(@Nullable final CharSequence title) {
        if (mBreadCrumbTitle == null) {
            setTitle(title);
            return;
        }
        if (mBreadCrumbTitle.getVisibility() != View.VISIBLE) {
            setTitle(title);
        } else {
            mBreadCrumbTitle.setText(title);
        }
    }

    void startPreferenceFragment(@NonNull final Preference pref) {
        final Fragment fragment = Fragment.instantiate(getContext(), pref.getFragment(), pref.getExtras());
        getFragmentManager().beginTransaction()
                .replace(R.id.prefs, fragment)
                .setBreadCrumbTitle(pref.getTitle())
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .addToBackStack(BACK_STACK_PREFS)
                .commitAllowingStateLoss();
    }

    @Nullable
    private Header findBestMatchingHeader(
            @NonNull final Header current,
            @NonNull final ArrayList<Header> from) {
        final ArrayList<Header> matches = new ArrayList<>();
        for (final Header oh : from) {
            if (current == oh || (current.id != HEADER_ID_UNDEFINED && current.id == oh.id)) {
                return oh;
            }
            if (current.fragment != null) {
                if (current.fragment.equals(oh.fragment)) {
                    matches.add(oh);
                }
            } else if (current.intent != null) {
                if (current.intent.equals(oh.intent)) {
                    matches.add(oh);
                }
            } else if (current.title != null) {
                if (current.title.equals(oh.title)) {
                    matches.add(oh);
                }
            }
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        for (final Header oh : matches) {
            if (current.fragmentArguments != null && current.fragmentArguments.equals(oh.fragmentArguments)) {
                return oh;
            }
            if (current.extras != null && current.extras.equals(oh.extras)) {
                return oh;
            }
            if (current.title != null && current.title.equals(oh.title)) {
                return oh;
            }
        }
        return null;
    }
}
