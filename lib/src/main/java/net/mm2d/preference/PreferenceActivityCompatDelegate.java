/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.arch.lifecycle.Lifecycle.State;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
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
        void onBuildHeaders(List<Header> target);

        boolean onIsMultiPane();

        boolean isValidFragment(String fragmentName);
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
    private final AdapterView.OnItemClickListener mOnClickListener = this::onListItemClick;
    @NonNull
    private final ArrayList<Header> mHeaders = new ArrayList<>();
    private ListAdapter mAdapter;
    private ListView mList;
    private boolean mFinishedStart = false;
    private FrameLayout mListFooter;
    private ViewGroup mPrefsContainer;
    private CharSequence mActivityTitle;
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

    public void onCreate(@Nullable final Bundle savedInstanceState) {
        mActivity.setContentView(R.layout.content);
        mList = mActivity.findViewById(R.id.list);
        mList.setOnItemClickListener(mOnClickListener);
        if (mFinishedStart) {
            setListAdapter(mAdapter);
        }
        mHandler.post(mRequestFocus);
        mFinishedStart = true;
        mListFooter = mActivity.findViewById(R.id.list_footer);
        mPrefsContainer = mActivity.findViewById(R.id.prefs_frame);
        mHeadersContainer = mActivity.findViewById(R.id.headers);
        mSinglePane = !mConnector.onIsMultiPane();
        final View breadCrumbSection = mActivity.findViewById(R.id.breadcrumb_section);
        mBreadCrumbTitle = mActivity.findViewById(R.id.bread_crumb_title);
        if (mSinglePane && breadCrumbSection != null && mBreadCrumbTitle != null) {
            mBreadCrumbTitle.setVisibility(View.GONE);
            breadCrumbSection.setVisibility(View.GONE);
        }
        mActivityTitle = mActivity.getTitle();
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
                showBreadCrumbs(mActivityTitle);
            }
        } else {
            mConnector.onBuildHeaders(mHeaders);
            if (!mSinglePane && mHeaders.size() > 0) {
                switchToHeader(onGetInitialHeader());
            }
        }
        if (mHeaders.size() > 0) {
            setListAdapter(new HeaderAdapter(mActivity, mHeaders, R.layout.header_item));
            if (!mSinglePane) {
                getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
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

    public void onSaveInstanceState(final Bundle outState) {
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

    public void onRestoreInstanceState(final Bundle state) {
        if (!mSinglePane) {
            if (mCurHeader != null) {
                setSelectedHeader(mCurHeader);
            }
        }
    }

    public boolean onBackPressed() {
        if (mCurHeader != null
                && mSinglePane
                && mActivity.getSupportFragmentManager().getBackStackEntryCount() == 0) {
            if (mFragment != null) {
                mActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .remove(mFragment)
                        .commitAllowingStateLoss();
                mFragment = null;
            }
            mCurHeader = null;
            mPrefsContainer.setVisibility(View.GONE);
            mHeadersContainer.setVisibility(View.VISIBLE);
            if (mActivityTitle != null) {
                showBreadCrumbs(mActivityTitle);
            }
            getListView().clearChoices();
            return true;
        }
        return false;
    }

    private void setListAdapter(final ListAdapter adapter) {
        mAdapter = adapter;
        mList.setAdapter(adapter);
    }

    public int getSelectedItemPosition() {
        return mList.getSelectedItemPosition();
    }

    private ListView getListView() {
        return mList;
    }

    public boolean hasHeaders() {
        return mHeadersContainer != null && mHeadersContainer.getVisibility() == View.VISIBLE;
    }

    public List<Header> getHeaders() {
        return mHeaders;
    }

    public boolean isMultiPane() {
        return !mSinglePane;
    }

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
        HeaderLoader.loadFromResource(mActivity, resId, target);
    }

    public void setListFooter(final View view) {
        mListFooter.removeAllViews();
        mListFooter.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    private void onListItemClick(
            final AdapterView<?> l,
            final View v,
            final int position,
            final long id) {
        if (!isResumed()) {
            return;
        }

        if (mAdapter != null) {
            final Object item = mAdapter.getItem(position);
            if (item instanceof Header) onHeaderClick((Header) item);
        }
    }

    private boolean isResumed() {
        return mActivity.getLifecycle().getCurrentState() == State.RESUMED;
    }

    private void onHeaderClick(final Header header) {
        if (header.fragment != null) {
            switchToHeader(header);
        } else if (header.intent != null) {
            mActivity.startActivity(header.intent);
        }
    }

    public void switchToHeader(final Header header) {
        if (mCurHeader == header) {
            mActivity.getSupportFragmentManager().popBackStack(BACK_STACK_PREFS, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            if (header.fragment == null) {
                throw new IllegalStateException("can't switch to header that has no fragment");
            }
            setSelectedHeader(header);
            mHandler.post(() -> switchToHeaderInner(header.fragment, header.fragmentArguments));
        }
    }

    private void switchToHeaderInner(
            final String fragmentName,
            final Bundle args) {
        final FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        fragmentManager.popBackStack(BACK_STACK_PREFS, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (!mConnector.isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: " + fragmentName);
        }

        final Fragment f = Fragment.instantiate(mActivity, fragmentName, args);
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(mSinglePane
                ? FragmentTransaction.TRANSIT_NONE
                : FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.replace(R.id.prefs, f);
        transaction.commitAllowingStateLoss();
        mFragment = f;

        if (mSinglePane && mPrefsContainer.getVisibility() == View.GONE) {
            mPrefsContainer.setVisibility(View.VISIBLE);
            mHeadersContainer.setVisibility(View.GONE);
        }
    }

    private void setSelectedHeader(final Header header) {
        mCurHeader = header;
        final int index = mHeaders.indexOf(header);
        if (index >= 0) {
            getListView().setItemChecked(index, true);
        } else {
            getListView().clearChoices();
        }
        showBreadCrumbs(header);
    }

    private void showBreadCrumbs(final Header header) {
        if (header != null) {
            CharSequence title = header.getBreadCrumbTitle(mActivity.getResources());
            if (title == null) title = header.getTitle(mActivity.getResources());
            if (title == null) title = mActivity.getTitle();
            showBreadCrumbs(title);
        } else {
            showBreadCrumbs(mActivity.getTitle());
        }
    }

    private void showBreadCrumbs(final CharSequence title) {
        if (mBreadCrumbTitle == null) {
            mActivity.setTitle(title);
            return;
        }
        if (mBreadCrumbTitle.getVisibility() != View.VISIBLE) {
            mActivity.setTitle(title);
        } else {
            mBreadCrumbTitle.setText(title);
        }
    }

    private Header findBestMatchingHeader(
            final Header cur,
            final ArrayList<Header> from) {
        final ArrayList<Header> matches = new ArrayList<>();
        for (int j = 0; j < from.size(); j++) {
            final Header oh = from.get(j);
            if (cur == oh || (cur.id != HEADER_ID_UNDEFINED && cur.id == oh.id)) {
                // Must be this one.
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
