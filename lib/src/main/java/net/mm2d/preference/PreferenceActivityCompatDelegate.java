/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.arch.lifecycle.Lifecycle.State;
import android.content.Intent;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
public class PreferenceActivityCompatDelegate {
    public interface Connector {
        void onBuildHeaders(List<Header> target);
        boolean isValidFragment(String fragmentName);
    }
    @NonNull
    private final FragmentActivity mActivity;
    @NonNull
    private final Connector mConnector;

    public PreferenceActivityCompatDelegate(
            @NonNull final FragmentActivity activity,
            @NonNull final Connector connector) {
        mActivity = activity;
        mConnector = connector;
    }

    private static final String HEADERS_TAG = ":android:headers";
    private static final String CUR_HEADER_TAG = ":android:cur_header";
    public static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":android:show_fragment_args";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":android:show_fragment_title";
    public static final String EXTRA_SHOW_FRAGMENT_SHORT_TITLE = ":android:show_fragment_short_title";
    public static final String EXTRA_NO_HEADERS = ":android:no_headers";
    private static final String BACK_STACK_PREFS = ":android:prefs";

    private ListAdapter mAdapter;
    private ListView mList;

    private boolean mFinishedStart = false;

    private final Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    private final AdapterView.OnItemClickListener mOnClickListener = this::onListItemClick;

    private final ArrayList<Header> mHeaders = new ArrayList<>();
    private FrameLayout mListFooter;
    private ViewGroup mPrefsContainer;
    private CharSequence mActivityTitle;
    private ViewGroup mHeadersContainer;
    private TextView mBreadCrumbTitle;
    private boolean mSinglePane;
    private Header mCurHeader;

    private final Runnable mBuildHeaders = new Runnable() {
        @Override
        public void run() {
            final ArrayList<Header> oldHeaders = new ArrayList<>(mHeaders);
            mHeaders.clear();
            mConnector.onBuildHeaders(mHeaders);
            if (mAdapter instanceof BaseAdapter) {
                ((BaseAdapter) mAdapter).notifyDataSetChanged();
            }
            final Header header = onGetNewHeader();
            if (header != null && header.fragment != null) {
                final Header mappedHeader = findBestMatchingHeader(header, oldHeaders);
                if (mappedHeader == null || mCurHeader != mappedHeader) {
                    switchToHeader(header);
                }
            } else if (mCurHeader != null) {
                final Header mappedHeader = findBestMatchingHeader(mCurHeader, mHeaders);
                if (mappedHeader != null) {
                    setSelectedHeader(mappedHeader);
                }
            }
        }
    };
    private final Handler mHandler = new Handler();
    private Fragment mFragment;

    public static final long HEADER_ID_UNDEFINED = -1;

    public void setListAdapter(final ListAdapter adapter) {
        mAdapter = adapter;
        mList.setAdapter(adapter);
    }

    public void setSelection(final int position) {
        mList.setSelection(position);
    }

    public int getSelectedItemPosition() {
        return mList.getSelectedItemPosition();
    }

    public ListView getListView() {
        return mList;
    }

    public ListAdapter getListAdapter() {
        return mAdapter;
    }

    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        mActivity.setContentView(R.layout.content_dual);
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
        final boolean hidingHeaders = onIsHidingHeaders();
        mSinglePane = hidingHeaders || !onIsMultiPane();
        final View breadCrumbSection = mActivity.findViewById(R.id.breadcrumb_section);
        mBreadCrumbTitle = mActivity.findViewById(R.id.bread_crumb_title);
        if (mSinglePane && breadCrumbSection != null && mBreadCrumbTitle != null) {
            mBreadCrumbTitle.setVisibility(View.GONE);
            breadCrumbSection.setVisibility(View.GONE);
        }
        final Intent intent = mActivity.getIntent();
        final String initialFragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);
        final Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        final int initialTitle =intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
        final int initialShortTitle = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, 0);
        mActivityTitle = mActivity.getTitle();
        if (savedInstanceState != null) {
            final ArrayList<Header> headers = savedInstanceState.getParcelableArrayList(HEADERS_TAG);
            if (headers != null) {
                mHeaders.addAll(headers);
                final int curHeader = savedInstanceState.getInt(CUR_HEADER_TAG,
                        (int) HEADER_ID_UNDEFINED);
                if (curHeader >= 0 && curHeader < mHeaders.size()) {
                    setSelectedHeader(mHeaders.get(curHeader));
                } else if (!mSinglePane && initialFragment == null) {
                    switchToHeader(onGetInitialHeader());
                }
            } else {
                showBreadCrumbs(mActivityTitle, null);
            }
        } else {
            if (!onIsHidingHeaders()) {
                mConnector.onBuildHeaders(mHeaders);
            }
            if (initialFragment != null) {
                switchToHeader(initialFragment, initialArguments);
            } else if (!mSinglePane && mHeaders.size() > 0) {
                switchToHeader(onGetInitialHeader());
            }
        }
        if (mHeaders.size() > 0) {
            setListAdapter(new HeaderAdapter(mActivity, mHeaders, R.layout.header_item));
            if (!mSinglePane) {
                getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            }
        }
        if (mSinglePane && initialFragment != null && initialTitle != 0) {
            final CharSequence initialTitleStr = mActivity.getText(initialTitle);
            final CharSequence initialShortTitleStr = initialShortTitle != 0
                    ? mActivity.getText(initialShortTitle) : null;
            showBreadCrumbs(initialTitleStr, initialShortTitleStr);
        }
        if (mHeaders.size() == 0 && initialFragment == null) {
            mActivity.setContentView(R.layout.content_single);
            mListFooter = mActivity.findViewById(R.id.list_footer);
            mPrefsContainer = mActivity.findViewById(R.id.prefs);
            mHeadersContainer = null;
        } else if (mSinglePane) {
            if (initialFragment != null || mCurHeader != null) {
                mHeadersContainer.setVisibility(View.GONE);
            } else {
                mPrefsContainer.setVisibility(View.GONE);
            }
            //final ViewGroup container = mActivity.findViewById(R.id.prefs_container);
            //container.setLayoutTransition(new LayoutTransition());
        } else {
            if (mHeaders.size() > 0 && mCurHeader != null) {
                setSelectedHeader(mCurHeader);
            }
        }
    }

    public boolean onBackPressed() {
        if (mCurHeader != null
                && mSinglePane
                && mActivity.getSupportFragmentManager().getBackStackEntryCount() == 0
                && mActivity.getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT) == null) {
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
                showBreadCrumbs(mActivityTitle, null);
            }
            getListView().clearChoices();
            return true;
        }
        return false;
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

    public boolean onIsMultiPane() {
        return mActivity.getResources().getBoolean(R.bool.dual_pane);
    }

    public boolean onIsHidingHeaders() {
        return mActivity.getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false);
    }

    public Header onGetInitialHeader() {
        for (int i = 0; i < mHeaders.size(); i++) {
            final Header h = mHeaders.get(i);
            if (h.fragment != null) {
                return h;
            }
        }
        throw new IllegalStateException("Must have at least one header with a fragment");
    }

    public Header onGetNewHeader() {
        return null;
    }

    public void invalidateHeaders() {
        mHandler.removeCallbacks(mBuildHeaders);
        mHandler.post(mBuildHeaders);
    }

    public void loadHeadersFromResource(
            @XmlRes int resId,
            final List<Header> target) {
        XmlResourceParser parser = null;
        try {
            parser = mActivity.getResources().getXml(resId);
            final AttributeSet attrs = Xml.asAttributeSet(parser);

            while (true) {
                final int type = parser.next();
                if (type == XmlPullParser.END_DOCUMENT ||
                        type == XmlPullParser.START_TAG) {
                    break;
                }
            }

            String nodeName = parser.getName();
            if (!"preference-headers".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <preference-headers> tag; found"
                                + nodeName + " at " + parser.getPositionDescription());
            }

            Bundle curBundle = null;

            final int outerDepth = parser.getDepth();
            while (true) {
                final int type = parser.next();
                if (type == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                if (type == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) {
                    break;
                }
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("header".equals(nodeName)) {
                    final Header header = new Header();

                    final TypedArray sa = mActivity.obtainStyledAttributes(attrs, R.styleable.PreferenceHeader);
                    header.id = sa.getResourceId(R.styleable.PreferenceHeader_id, (int) HEADER_ID_UNDEFINED);
                    TypedValue tv = sa.peekValue(R.styleable.PreferenceHeader_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.titleRes = tv.resourceId;
                        } else {
                            header.title = tv.string;
                        }
                    }
                    tv = sa.peekValue(R.styleable.PreferenceHeader_summary);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.summaryRes = tv.resourceId;
                        } else {
                            header.summary = tv.string;
                        }
                    }
                    tv = sa.peekValue(R.styleable.PreferenceHeader_breadCrumbTitle);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.breadCrumbTitleRes = tv.resourceId;
                        } else {
                            header.breadCrumbTitle = tv.string;
                        }
                    }
                    tv = sa.peekValue(R.styleable.PreferenceHeader_breadCrumbShortTitle);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.breadCrumbShortTitleRes = tv.resourceId;
                        } else {
                            header.breadCrumbShortTitle = tv.string;
                        }
                    }
                    header.iconRes = sa.getResourceId(R.styleable.PreferenceHeader_icon, 0);
                    header.fragment = sa.getString(R.styleable.PreferenceHeader_fragment);
                    sa.recycle();

                    if (curBundle == null) {
                        curBundle = new Bundle();
                    }

                    final int innerDepth = parser.getDepth();
                    while (true) {
                        final int type2 = parser.next();
                        if (type2 == XmlPullParser.END_DOCUMENT) {
                            break;
                        }
                        if (type2 == XmlPullParser.END_TAG && parser.getDepth() <= innerDepth) {
                            break;
                        }
                        if (type2 == XmlPullParser.END_TAG || type2 == XmlPullParser.TEXT) {
                            continue;
                        }

                        final String innerNodeName = parser.getName();
                        switch (innerNodeName) {
                            case "extra":
                                mActivity.getResources().parseBundleExtra("extra", attrs, curBundle);
                                skipCurrentTag(parser);
                                break;
                            case "intent":
                                header.intent = Intent.parseIntent(mActivity.getResources(), parser, attrs);
                                break;
                            default:
                                skipCurrentTag(parser);
                                break;
                        }
                    }

                    if (curBundle.size() > 0) {
                        header.fragmentArguments = curBundle;
                        curBundle = null;
                    }

                    target.add(header);
                } else {
                    skipCurrentTag(parser);
                }
            }

        } catch (final XmlPullParserException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (final IOException e) {
            throw new RuntimeException("Error parsing headers", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    public void setListFooter(final View view) {
        mListFooter.removeAllViews();
        mListFooter.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
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
            if (item instanceof Header) onHeaderClick((Header) item, position);
        }
    }

    private boolean isResumed() {
        return mActivity.getLifecycle().getCurrentState() == State.RESUMED;
    }

    public void onHeaderClick(
            final Header header,
            final int position) {
        if (header.fragment != null) {
            switchToHeader(header);
        } else if (header.intent != null) {
            mActivity.startActivity(header.intent);
        }
    }

    public void showBreadCrumbs(
            final CharSequence title,
            final CharSequence shortTitle) {
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
            showBreadCrumbs(title, header.getBreadCrumbShortTitle(mActivity.getResources()));
        } else {
            showBreadCrumbs(mActivity.getTitle(), null);
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

    public void switchToHeader(
            final String fragmentName,
            final Bundle args) {
        Header selectedHeader = null;
        for (int i = 0; i < mHeaders.size(); i++) {
            if (fragmentName.equals(mHeaders.get(i).fragment)) {
                selectedHeader = mHeaders.get(i);
                break;
            }
        }
        setSelectedHeader(selectedHeader);
        mHandler.post(() -> switchToHeaderInner(fragmentName, args));
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

    private static void skipCurrentTag(final XmlPullParser parser) throws XmlPullParserException, IOException {
        final int outerDepth = parser.getDepth();
        while (true) {
            final int type = parser.next();
            if (type == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (type == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) {
                break;
            }
        }
    }
}
