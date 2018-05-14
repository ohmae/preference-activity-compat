/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.TintTypedArray;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Objects;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class HeaderAdapter extends ArrayAdapter<Header> {
    private static class HeaderViewHolder {
        ImageView icon;
        TextView title;
        TextView summary;
    }

    private final int mColorAccent;
    @NonNull
    private final LayoutInflater mInflater;

    HeaderAdapter(
            @NonNull final Context context,
            @NonNull final List<Header> objects) {
        super(context, 0, objects);
        final Object service = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater = (LayoutInflater) Objects.requireNonNull(service);
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
        final HeaderViewHolder holder;
        final View view;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.header_item, parent, false);
            setBackground(view);
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
        assert header != null;
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
