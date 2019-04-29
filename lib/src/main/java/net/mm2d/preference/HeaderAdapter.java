/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TintTypedArray;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class HeaderAdapter extends ArrayAdapter<Header> {
    private static class ViewHolder {
        private final ImageView mIcon;
        private final TextView mTitle;
        private final TextView mSummary;

        ViewHolder(
                @NonNull final ImageView icon,
                @NonNull final TextView title,
                @NonNull final TextView summary) {
            mIcon = icon;
            mTitle = title;
            mSummary = summary;
        }

        ImageView getIcon() {
            return mIcon;
        }

        TextView getTitle() {
            return mTitle;
        }

        TextView getSummary() {
            return mSummary;
        }
    }

    private final int mColorAccent;
    @NonNull
    private final LayoutInflater mInflater;

    HeaderAdapter(
            @NonNull final Context context,
            @NonNull final List<Header> objects) {
        super(context, 0, objects);
        mInflater = LayoutInflater.from(context);
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            mColorAccent = getThemeAttrColor(context, R.attr.colorAccent);
        } else {
            mColorAccent = 0;
        }
    }

    @NonNull
    @Override
    public View getView(
            final int position,
            @Nullable final View convertView,
            @NonNull final ViewGroup parent) {
        final ViewHolder holder;
        final View view;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.mm2d_pac_header_item, parent, false);
            setBackground(view);
            holder = new ViewHolder(
                    view.findViewById(R.id.icon),
                    view.findViewById(R.id.title),
                    view.findViewById(R.id.summary));
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

        final ImageView icon = holder.getIcon();
        final Header header = getItem(position);
        if (header.iconRes == 0) {
            icon.setVisibility(View.GONE);
        } else {
            icon.setVisibility(View.VISIBLE);
            icon.setImageResource(header.iconRes);
        }
        final Resources resources = getContext().getResources();
        holder.getTitle().setText(header.getTitle(resources));
        final TextView summary = holder.getSummary();
        final CharSequence text = header.getSummary(resources);
        if (!TextUtils.isEmpty(text)) {
            summary.setVisibility(View.VISIBLE);
            summary.setText(text);
        } else {
            summary.setVisibility(View.GONE);
        }
        return view;
    }

    @SuppressLint("RestrictedApi")
    private static int getThemeAttrColor(
            @NonNull final Context context,
            final int attr) {
        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, null, new int[]{attr});
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    private void setBackground(@NonNull final View view) {
        if (mColorAccent == 0) {
            return;
        }
        final StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_activated}, new ColorDrawable(mColorAccent));
        drawable.addState(new int[]{-android.R.attr.state_activated}, new ColorDrawable(Color.TRANSPARENT));
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }
}
