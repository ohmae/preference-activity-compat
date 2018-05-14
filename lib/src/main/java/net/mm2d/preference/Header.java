/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
public final class Header implements Parcelable {
    public long id = PreferenceActivityCompatDelegate.HEADER_ID_UNDEFINED;
    @StringRes
    public int titleRes;
    public CharSequence title;
    @StringRes
    public int summaryRes;
    public CharSequence summary;
    @StringRes
    public int breadCrumbTitleRes;
    public CharSequence breadCrumbTitle;
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
