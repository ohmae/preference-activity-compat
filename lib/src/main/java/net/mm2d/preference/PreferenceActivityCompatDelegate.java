/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.arch.lifecycle.Lifecycle.State;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.Preference;
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

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
public class PreferenceActivityCompatDelegate {
    public interface Connector {
        void onBuildHeaders(@NonNull List<Header> target);

        boolean onIsMultiPane();

        boolean isValidFragment(@Nullable String fragmentName);
    }

    public static final long HEADER_ID_UNDEFINED = -1;

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
    private Header mCurHeader;
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
            if (mCurHeader != null) {
                final Header mappedHeader = findBestMatchingHeader(mCurHeader, mHeaders);
                if (mappedHeader != null) {
                    setSelectedHeader(mappedHeader);
                }
            }
        }
    };

    public PreferenceActivityCompatDelegate(
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

    public void setContentView(@LayoutRes final int layoutResID) {
        mActivity.setContentView(layoutResID);
    }

    @Nullable
    private <T extends View> T findViewById(@IdRes final int id) {
        return mActivity.findViewById(id);
    }

    public void onCreate(@Nullable final Bundle savedInstanceState) {
        setContentView(R.layout.content);
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
        mSinglePane = !mConnector.onIsMultiPane();
        final View breadCrumbSection = findViewById(R.id.breadcrumb_section);
        mBreadCrumbTitle = findViewById(R.id.bread_crumb_title);
        if (mSinglePane && breadCrumbSection != null && mBreadCrumbTitle != null) {
            mBreadCrumbTitle.setVisibility(View.GONE);
            breadCrumbSection.setVisibility(View.GONE);
        }
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
            mConnector.onBuildHeaders(mHeaders);
            if (!mSinglePane && mHeaders.size() > 0) {
                switchToHeader(onGetInitialHeader());
            }
        }
        if (mHeaders.size() > 0) {
            setListAdapter(new HeaderAdapter(getContext(), mHeaders));
            if (!mSinglePane) {
                mList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            }
        }
        if (mSinglePane) {
            if (mCurHeader != null) {
                mHeadersContainer.setVisibility(View.GONE);
            } else {
                mPrefsContainer.setVisibility(View.GONE);
            }
        } else if (mHeaders.size() > 0 && mCurHeader != null) {
            setSelectedHeader(mCurHeader);
        }
    }

    public void onDestroy() {
        mHandler.removeCallbacks(mBuildHeaders);
        mHandler.removeCallbacks(mRequestFocus);
    }

    public void onSaveInstanceState(@NonNull final Bundle outState) {
        if (mHeaders.size() > 0) {
            outState.putParcelableArrayList(HEADERS_TAG, mHeaders);
            if (mCurHeader != null) {
                final int index = mHeaders.indexOf(mCurHeader);
                if (index >= 0) {
                    outState.putInt(CUR_HEADER_TAG, index);
                }
            }
        }
    }

    public void onRestoreInstanceState(@NonNull final Bundle state) {
        if (!mSinglePane) {
            if (mCurHeader != null) {
                setSelectedHeader(mCurHeader);
            }
        }
    }

    public boolean onBackPressed() {
        final FragmentManager manager = getFragmentManager();
        if (mCurHeader == null || !mSinglePane || manager.getBackStackEntryCount() != 0) {
            return false;
        }
        if (mFragment != null) {
            manager.beginTransaction()
                    .remove(mFragment)
                    .commitAllowingStateLoss();
            mFragment = null;
        }
        mCurHeader = null;
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

    public int getSelectedItemPosition() {
        return mList.getSelectedItemPosition();
    }

    public boolean hasHeaders() {
        return mHeadersContainer != null && mHeadersContainer.getVisibility() == View.VISIBLE;
    }

    @NonNull
    public List<Header> getHeaders() {
        return mHeaders;
    }

    public boolean isMultiPane() {
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

    public void invalidateHeaders() {
        mHandler.removeCallbacks(mBuildHeaders);
        mHandler.post(mBuildHeaders);
    }

    public void loadHeadersFromResource(
            @XmlRes final int resId,
            @NonNull final List<Header> target) {
        HeaderLoader.loadFromResource(getContext(), resId, target);
    }

    public void setListFooter(@NonNull final View view) {
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

    public void switchToHeader(@NonNull final Header header) {
        if (mCurHeader == header) {
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
        mCurHeader = header;
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

    public void startPreferenceFragment(@NonNull final Preference pref) {
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
            @NonNull final Header cur,
            @NonNull final ArrayList<Header> from) {
        final ArrayList<Header> matches = new ArrayList<>();
        for (int j = 0; j < from.size(); j++) {
            final Header oh = from.get(j);
            if (cur == oh || (cur.id != HEADER_ID_UNDEFINED && cur.id == oh.id)) {
                matches.clear();
                matches.add(oh);
                break;
            }
            if (cur.fragment != null) {
                if (cur.fragment.equals(oh.fragment)) {
                    matches.add(oh);
                }
            } else if (cur.intent != null) {
                if (cur.intent.equals(oh.intent)) {
                    matches.add(oh);
                }
            } else if (cur.title != null) {
                if (cur.title.equals(oh.title)) {
                    matches.add(oh);
                }
            }
        }
        final int NM = matches.size();
        if (NM == 1) {
            return matches.get(0);
        } else if (NM > 1) {
            for (int j = 0; j < NM; j++) {
                final Header oh = matches.get(j);
                if (cur.fragmentArguments != null &&
                        cur.fragmentArguments.equals(oh.fragmentArguments)) {
                    return oh;
                }
                if (cur.extras != null && cur.extras.equals(oh.extras)) {
                    return oh;
                }
                if (cur.title != null && cur.title.equals(oh.title)) {
                    return oh;
                }
            }
        }
        return null;
    }
}
