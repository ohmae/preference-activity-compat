/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class HeaderLoader {
    static void loadFromResource(
            @NonNull final Context context,
            @XmlRes final int resId,
            @NonNull final List<Header> target) {
        XmlResourceParser parser = null;
        try {
            parser = context.getResources().getXml(resId);
            loadFromResource(context, parser, target);
        } catch (final XmlPullParserException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (final IOException e) {
            throw new RuntimeException("Error parsing headers", e);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private static void loadFromResource(
            @NonNull final Context context,
            @NonNull final XmlResourceParser parser,
            @NonNull final List<Header> target)
            throws IOException, XmlPullParserException {
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
                target.add(parseHeaderSection(context, parser, attrs));
            } else {
                skipCurrentTag(parser);
            }
        }
    }

    private static Header parseHeaderSection(
            @NonNull final Context context,
            @NonNull final XmlResourceParser parser,
            @NonNull final AttributeSet attrs)
            throws IOException, XmlPullParserException {
        final Header header = new Header();
        final TypedArray sa = context.obtainStyledAttributes(attrs, R.styleable.PreferenceHeader);
        header.id = sa.getResourceId(R.styleable.PreferenceHeader_id, (int) PreferenceActivityCompatDelegate.HEADER_ID_UNDEFINED);
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
        header.iconRes = sa.getResourceId(R.styleable.PreferenceHeader_icon, 0);
        header.fragment = sa.getString(R.styleable.PreferenceHeader_fragment);
        sa.recycle();
        parseIntentSection(context, parser, attrs, header);
        return header;
    }

    private static void parseIntentSection(
            @NonNull final Context context,
            @NonNull final XmlResourceParser parser,
            @NonNull final AttributeSet attrs,
            @NonNull final Header header)
            throws IOException, XmlPullParserException {
        final Bundle curBundle = new Bundle();
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
                    context.getResources().parseBundleExtra("extra", attrs, curBundle);
                    skipCurrentTag(parser);
                    break;
                case "intent":
                    header.intent = Intent.parseIntent(context.getResources(), parser, attrs);
                    break;
                default:
                    skipCurrentTag(parser);
                    break;
            }
        }
        if (curBundle.size() > 0) {
            header.fragmentArguments = curBundle;
        }
    }

    private static void skipCurrentTag(final XmlPullParser parser)
            throws IOException, XmlPullParserException {
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
