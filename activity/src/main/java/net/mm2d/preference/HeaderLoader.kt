/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.XmlResourceParser
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.util.Xml
import androidx.annotation.XmlRes
import androidx.core.content.res.use
import net.mm2d.preference.PreferenceActivityCompatDelegate.Companion.HEADER_ID_UNDEFINED
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

internal object HeaderLoader {
    fun loadFromResource(
        context: Context,
        @XmlRes resId: Int,
        target: MutableList<Header>
    ) {
        var parser: XmlResourceParser? = null
        try {
            parser = context.resources.getXml(resId)
            loadFromResource(context, parser, target)
        } finally {
            parser?.close()
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun loadFromResource(
        context: Context,
        parser: XmlResourceParser,
        target: MutableList<Header>
    ) {
        val attrs = Xml.asAttributeSet(parser)
        @Suppress("ControlFlowWithEmptyBody")
        while (parser.next().let { it != XmlPullParser.END_DOCUMENT && it != XmlPullParser.START_TAG });
        if ("preference-headers" != parser.name) {
            throw RuntimeException(
                "XML document must start with <preference-headers> tag; found"
                    + parser.name + " at " + parser.positionDescription
            )
        }
        val startDepth = parser.depth
        while (true) {
            val type = parser.next()
            if (reachToEnd(type, parser.depth, startDepth)) {
                break
            }
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue
            }
            if ("header" == parser.name) {
                target.add(parseHeaderSection(context, parser, attrs))
            } else {
                skipCurrentTag(parser)
            }
        }
    }

    @SuppressLint("Recycle")
    @Throws(IOException::class, XmlPullParserException::class)
    private fun parseHeaderSection(
        context: Context,
        parser: XmlResourceParser,
        attrs: AttributeSet
    ): Header {
        val header = Header()
        context.obtainStyledAttributes(attrs, R.styleable.PreferenceHeader).use {
            header.id = it.getResourceId(R.styleable.PreferenceHeader_id, HEADER_ID_UNDEFINED)
            header.setTitle(it.peekValue(R.styleable.PreferenceHeader_title))
            header.setSummary(it.peekValue(R.styleable.PreferenceHeader_summary))
            header.setBreadCrumbTitle(it.peekValue(R.styleable.PreferenceHeader_breadCrumbTitle))
            header.iconRes = it.getResourceId(R.styleable.PreferenceHeader_icon, 0)
            header.fragment = it.getString(R.styleable.PreferenceHeader_fragment)
        }
        parseIntentSection(context, parser, attrs, header)
        return header
    }

    private fun Header.setTitle(tv: TypedValue?) {
        if (tv?.type != TypedValue.TYPE_STRING) return
        if (tv.resourceId != 0) {
            titleRes = tv.resourceId
        } else {
            title = tv.string
        }
    }

    private fun Header.setSummary(tv: TypedValue?) {
        if (tv?.type != TypedValue.TYPE_STRING) return
        if (tv.resourceId != 0) {
            summaryRes = tv.resourceId
        } else {
            summary = tv.string
        }
    }

    private fun Header.setBreadCrumbTitle(tv: TypedValue?) {
        if (tv?.type != TypedValue.TYPE_STRING) return
        if (tv.resourceId != 0) {
            breadCrumbTitleRes = tv.resourceId
        } else {
            breadCrumbTitle = tv.string
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun parseIntentSection(
        context: Context,
        parser: XmlResourceParser,
        attrs: AttributeSet,
        header: Header
    ) {
        val bundle = Bundle()
        val startDepth = parser.depth
        while (true) {
            val type = parser.next()
            if (reachToEnd(type, parser.depth, startDepth)) {
                break
            }
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue
            }
            when (parser.name) {
                "extra" -> {
                    context.resources.parseBundleExtra("extra", attrs, bundle)
                    skipCurrentTag(parser)
                }
                "intent" -> header.intent = Intent.parseIntent(context.resources, parser, attrs)
                else -> skipCurrentTag(parser)
            }
        }
        if (bundle.size() > 0) {
            header.fragmentArguments = bundle
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun skipCurrentTag(parser: XmlPullParser) {
        val startDepth = parser.depth
        @Suppress("ControlFlowWithEmptyBody")
        while (!reachToEnd(parser.next(), parser.depth, startDepth));
    }

    private fun reachToEnd(
        type: Int,
        currentDepth: Int,
        startDepth: Int
    ): Boolean = type == XmlPullParser.END_DOCUMENT ||
        type == XmlPullParser.END_TAG && currentDepth <= startDepth
}
