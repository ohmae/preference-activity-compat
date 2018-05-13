/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class HeaderAdapter extends ArrayAdapter<Header> {
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
