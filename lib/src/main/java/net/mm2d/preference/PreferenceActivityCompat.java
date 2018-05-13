/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.FragmentBreadCrumbs;
import android.arch.lifecycle.Lifecycle.State;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
@SuppressLint("Registered")
public class PreferenceActivityCompat extends AppCompatActivity {
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
    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private boolean mSinglePane;
    private Header mCurHeader;

    private final Runnable mBuildHeaders = new Runnable() {
        @Override
        public void run() {
            final ArrayList<Header> oldHeaders = new ArrayList<>(mHeaders);
            mHeaders.clear();
            onBuildHeaders(mHeaders);
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

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private static class HeaderViewHolder {
            ImageView icon;
            TextView title;
            TextView summary;
        }

        private final LayoutInflater mInflater;
        private final int mLayoutResId;

        public HeaderAdapter(
                final Context context,
                final List<Header> objects,
                final int layoutResId) {
            super(context, 0, objects);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayoutResId = layoutResId;
        }

        @NonNull
        @Override
        public View getView(
                final int position,
                @Nullable final View convertView,
                @NonNull final ViewGroup parent) {
            final HeaderViewHolder holder;
            final View view;

            if (convertView == null) {
                view = mInflater.inflate(mLayoutResId, parent, false);
                holder = new HeaderViewHolder();
                holder.icon = view.findViewById(R.id.icon);
                holder.title = view.findViewById(R.id.title);
                holder.summary = view.findViewById(R.id.summary);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            final Header header = getItem(position);
            if (header.iconRes == 0) {
                holder.icon.setVisibility(View.GONE);
            } else {
                holder.icon.setVisibility(View.VISIBLE);
                holder.icon.setImageResource(header.iconRes);
            }
            holder.title.setText(header.getTitle(getContext().getResources()));
            final CharSequence summary = header.getSummary(getContext().getResources());
            if (!TextUtils.isEmpty(summary)) {
                holder.summary.setVisibility(View.VISIBLE);
                holder.summary.setText(summary);
            } else {
                holder.summary.setVisibility(View.GONE);
            }

            return view;
        }
    }

    public static final long HEADER_ID_UNDEFINED = -1;

    public static final class Header implements Parcelable {
        public long id = HEADER_ID_UNDEFINED;

        @StringRes
        public int titleRes;

        public CharSequence title;

        @StringRes
        public int summaryRes;
        public CharSequence summary;
        @StringRes
        public int breadCrumbTitleRes;
        public CharSequence breadCrumbTitle;
        @StringRes
        public int breadCrumbShortTitleRes;
        public CharSequence breadCrumbShortTitle;
        public int iconRes;
        public String fragment;
        public Bundle fragmentArguments;
        public Intent intent;
        public Bundle extras;

        public Header() {
        }

        public CharSequence getTitle(final Resources res) {
            if (titleRes != 0) {
                return res.getText(titleRes);
            }
            return title;
        }

        public CharSequence getSummary(final Resources res) {
            if (summaryRes != 0) {
                return res.getText(summaryRes);
            }
            return summary;
        }

        public CharSequence getBreadCrumbTitle(final Resources res) {
            if (breadCrumbTitleRes != 0) {
                return res.getText(breadCrumbTitleRes);
            }
            return breadCrumbTitle;
        }

        public CharSequence getBreadCrumbShortTitle(final Resources res) {
            if (breadCrumbShortTitleRes != 0) {
                return res.getText(breadCrumbShortTitleRes);
            }
            return breadCrumbShortTitle;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(
                final Parcel dest,
                final int flags) {
            dest.writeLong(id);
            dest.writeInt(titleRes);
            TextUtils.writeToParcel(title, dest, flags);
            dest.writeInt(summaryRes);
            TextUtils.writeToParcel(summary, dest, flags);
            dest.writeInt(breadCrumbTitleRes);
            TextUtils.writeToParcel(breadCrumbTitle, dest, flags);
            dest.writeInt(breadCrumbShortTitleRes);
            TextUtils.writeToParcel(breadCrumbShortTitle, dest, flags);
            dest.writeInt(iconRes);
            dest.writeString(fragment);
            dest.writeBundle(fragmentArguments);
            if (intent != null) {
                dest.writeInt(1);
                intent.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
            dest.writeBundle(extras);
        }

        public void readFromParcel(final Parcel in) {
            id = in.readLong();
            titleRes = in.readInt();
            title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            summaryRes = in.readInt();
            summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            breadCrumbTitleRes = in.readInt();
            breadCrumbTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            breadCrumbShortTitleRes = in.readInt();
            breadCrumbShortTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            iconRes = in.readInt();
            fragment = in.readString();
            fragmentArguments = in.readBundle(getClass().getClassLoader());
            if (in.readInt() != 0) {
                intent = Intent.CREATOR.createFromParcel(in);
            }
            extras = in.readBundle(getClass().getClassLoader());
        }

        Header(final Parcel in) {
            readFromParcel(in);
        }

        public static final Creator<Header> CREATOR = new Creator<Header>() {
            public Header createFromParcel(final Parcel source) {
                return new Header(source);
            }

            public Header[] newArray(final int size) {
                return new Header[size];
            }
        };
    }

    public void setListAdapter(final ListAdapter adapter) {
        synchronized (this) {
            mAdapter = adapter;
            mList.setAdapter(adapter);
        }
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
        setContentView(R.layout.content_dual);
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
        final boolean hidingHeaders = onIsHidingHeaders();
        mSinglePane = hidingHeaders || !onIsMultiPane();
        final String initialFragment = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        final Bundle initialArguments = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        final int initialTitle = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
        final int initialShortTitle = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, 0);
        mActivityTitle = getTitle();
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
                showBreadCrumbs(getTitle(), null);
            }
        } else {
            if (!onIsHidingHeaders()) {
                onBuildHeaders(mHeaders);
            }
            if (initialFragment != null) {
                switchToHeader(initialFragment, initialArguments);
            } else if (!mSinglePane && mHeaders.size() > 0) {
                switchToHeader(onGetInitialHeader());
            }
        }
        if (mHeaders.size() > 0) {
            setListAdapter(new HeaderAdapter(this, mHeaders, R.layout.header_item));
            if (!mSinglePane) {
                getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            }
        }
        if (mSinglePane && initialFragment != null && initialTitle != 0) {
            final CharSequence initialTitleStr = getText(initialTitle);
            final CharSequence initialShortTitleStr = initialShortTitle != 0
                    ? getText(initialShortTitle) : null;
            showBreadCrumbs(initialTitleStr, initialShortTitleStr);
        }
        if (mHeaders.size() == 0 && initialFragment == null) {
            setContentView(R.layout.content_single);
            mListFooter = findViewById(R.id.list_footer);
            mPrefsContainer = findViewById(R.id.prefs);
            mHeadersContainer = null;
        } else if (mSinglePane) {
            if (initialFragment != null || mCurHeader != null) {
                mHeadersContainer.setVisibility(View.GONE);
            } else {
                mPrefsContainer.setVisibility(View.GONE);
            }
            final ViewGroup container = findViewById(R.id.prefs_container);
            container.setLayoutTransition(new LayoutTransition());
        } else {
            if (mHeaders.size() > 0 && mCurHeader != null) {
                setSelectedHeader(mCurHeader);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurHeader != null && mSinglePane && getFragmentManager().getBackStackEntryCount() == 0
                && getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT) == null) {
            mCurHeader = null;

            mPrefsContainer.setVisibility(View.GONE);
            mHeadersContainer.setVisibility(View.VISIBLE);
            if (mActivityTitle != null) {
                showBreadCrumbs(mActivityTitle, null);
            }
            getListView().clearChoices();
        } else {
            super.onBackPressed();
        }
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
        return getResources().getBoolean(R.bool.dual_pane);
    }

    public boolean onIsHidingHeaders() {
        return getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false);
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

    public void onBuildHeaders(final List<Header> target) {
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
            parser = getResources().getXml(resId);
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

                    final TypedArray sa = obtainStyledAttributes(attrs, R.styleable.PreferenceHeader);
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
                                getResources().parseBundleExtra("extra", attrs, curBundle);
                                skipCurrentTag(parser);
                                break;
                            case "intent":
                                header.intent = Intent.parseIntent(getResources(), parser, attrs);
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

    protected boolean isValidFragment(final String fragmentName) {
        if (getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.KITKAT) {
            throw new RuntimeException(
                    "Subclasses of PreferenceActivity must override isValidFragment(String)"
                            + " to verify that the Fragment class is valid! "
                            + this.getClass().getName()
                            + " has not checked if fragment " + fragmentName + " is valid.");
        } else {
            return true;
        }
    }

    public void setListFooter(final View view) {
        mListFooter.removeAllViews();
        mListFooter.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mBuildHeaders);
        mHandler.removeCallbacks(mRequestFocus);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

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

    @Override
    protected void onRestoreInstanceState(final Bundle state) {
        super.onRestoreInstanceState(state);

        if (!mSinglePane) {
            if (mCurHeader != null) {
                setSelectedHeader(mCurHeader);
            }
        }
    }

    protected void onListItemClick(
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
        return getLifecycle().getCurrentState() == State.RESUMED;
    }

    public void onHeaderClick(
            final Header header,
            final int position) {
        if (header.fragment != null) {
            switchToHeader(header);
        } else if (header.intent != null) {
            startActivity(header.intent);
        }
    }

    public void showBreadCrumbs(
            final CharSequence title,
            final CharSequence shortTitle) {
        if (mFragmentBreadCrumbs == null) {
            final View crumbs = findViewById(R.id.title);
            try {
                mFragmentBreadCrumbs = (FragmentBreadCrumbs) crumbs;
            } catch (final ClassCastException e) {
                setTitle(title);
                return;
            }
            if (mFragmentBreadCrumbs == null) {
                if (title != null) {
                    setTitle(title);
                }
                return;
            }
            if (mSinglePane) {
                mFragmentBreadCrumbs.setVisibility(View.GONE);
                final View bcSection = findViewById(R.id.breadcrumb_section);
                if (bcSection != null) bcSection.setVisibility(View.GONE);
                setTitle(title);
            }
            mFragmentBreadCrumbs.setMaxVisible(2);
            mFragmentBreadCrumbs.setActivity(this);
        }
        if (mFragmentBreadCrumbs.getVisibility() != View.VISIBLE) {
            setTitle(title);
        } else {
            mFragmentBreadCrumbs.setTitle(title, shortTitle);
            mFragmentBreadCrumbs.setParentTitle(null, null, null);
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
            CharSequence title = header.getBreadCrumbTitle(getResources());
            if (title == null) title = header.getTitle(getResources());
            if (title == null) title = getTitle();
            showBreadCrumbs(title, header.getBreadCrumbShortTitle(getResources()));
        } else {
            showBreadCrumbs(getTitle(), null);
        }
    }

    private void switchToHeaderInner(
            final String fragmentName,
            final Bundle args) {
        getFragmentManager().popBackStack(BACK_STACK_PREFS, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (!isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: " + fragmentName);
        }

        final Fragment f = Fragment.instantiate(this, fragmentName, args);
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(mSinglePane
                ? FragmentTransaction.TRANSIT_NONE
                : FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.replace(R.id.prefs, f);
        transaction.commitAllowingStateLoss();

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
        switchToHeaderInner(fragmentName, args);
    }

    public void switchToHeader(final Header header) {
        if (mCurHeader == header) {
            getFragmentManager().popBackStack(BACK_STACK_PREFS, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            if (header.fragment == null) {
                throw new IllegalStateException("can't switch to header that has no fragment");
            }
            switchToHeaderInner(header.fragment, header.fragmentArguments);
            setSelectedHeader(header);
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
