/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.TintTypedArray

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class HeaderAdapter(
    context: Context,
    objects: List<Header>
) : ArrayAdapter<Header>(context, 0, objects) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val colorAccent: Int = getColorAccent(context)

    @SuppressLint("RestrictedApi")
    private fun getColorAccent(context: Context): Int {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) return 0
        val a = TintTypedArray.obtainStyledAttributes(context, null, intArrayOf(R.attr.colorAccent))
        try {
            return a.getColor(0, 0)
        } finally {
            a.recycle()
        }
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = inflater.inflate(R.layout.mm2d_pac_header_item, parent, false)
            setBackground(view)
            holder = ViewHolder(
                view.findViewById(R.id.icon),
                view.findViewById(R.id.title),
                view.findViewById(R.id.summary)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val header = getItem(position) ?: return view
        if (header.iconRes == 0) {
            holder.icon.visibility = View.GONE
        } else {
            holder.icon.visibility = View.VISIBLE
            holder.icon.setImageResource(header.iconRes)
        }
        val resources = context.resources
        holder.title.text = header.getTitle(resources)
        val summary = holder.summary
        val text = header.getSummary(resources)
        if (text.isNullOrEmpty()) {
            summary.visibility = View.GONE
        } else {
            summary.visibility = View.VISIBLE
            summary.text = text
        }
        return view
    }

    private fun setBackground(view: View) {
        if (colorAccent == 0) return
        val drawable = StateListDrawable().also {
            it.addState(
                intArrayOf(android.R.attr.state_activated),
                ColorDrawable(colorAccent)
            )
            it.addState(
                intArrayOf(-android.R.attr.state_activated),
                ColorDrawable(Color.TRANSPARENT)
            )
        }
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            view.background = drawable
        } else {
            @Suppress("DEPRECATION")
            view.setBackgroundDrawable(drawable)
        }
    }

    private class ViewHolder internal constructor(
        internal val icon: ImageView,
        internal val title: TextView,
        internal val summary: TextView
    )
}
