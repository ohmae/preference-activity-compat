/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils

import androidx.annotation.StringRes
import net.mm2d.preference.PreferenceActivityCompatDelegate.Companion.HEADER_ID_UNDEFINED

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class Header : Parcelable {
    var id = HEADER_ID_UNDEFINED
    @StringRes
    var titleRes: Int = 0
    var title: CharSequence? = null
    @StringRes
    var summaryRes: Int = 0
    var summary: CharSequence? = null
    @StringRes
    var breadCrumbTitleRes: Int = 0
    var breadCrumbTitle: CharSequence? = null
    var iconRes: Int = 0
    var fragment: String? = null
    var fragmentArguments: Bundle? = null
    var intent: Intent? = null
    var extras: Bundle? = null

    constructor()

    private constructor(parcel: Parcel) {
        readFromParcel(parcel)
    }

    fun getTitle(res: Resources): CharSequence? =
        getText(res, titleRes, title)

    fun getSummary(res: Resources): CharSequence? =
        getText(res, summaryRes, summary)

    fun getBreadCrumbTitle(res: Resources): CharSequence? =
        getText(res, breadCrumbTitleRes, breadCrumbTitle)

    private fun getText(res: Resources, resId: Int, text: CharSequence?): CharSequence? =
        if (resId != 0) res.getText(resId) else text

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(titleRes)
        parcel.writeCharSequence(title, flags)
        parcel.writeInt(summaryRes)
        parcel.writeCharSequence(summary, flags)
        parcel.writeInt(breadCrumbTitleRes)
        parcel.writeCharSequence(breadCrumbTitle, flags)
        parcel.writeInt(iconRes)
        parcel.writeString(fragment)
        parcel.writeBundle(fragmentArguments)
        parcel.writeIntent(intent, flags)
        parcel.writeBundle(extras)
    }

    private fun readFromParcel(parcel: Parcel) {
        id = parcel.readInt()
        titleRes = parcel.readInt()
        title = parcel.readCharSequence()
        summaryRes = parcel.readInt()
        summary = parcel.readCharSequence()
        breadCrumbTitleRes = parcel.readInt()
        breadCrumbTitle = parcel.readCharSequence()
        iconRes = parcel.readInt()
        fragment = parcel.readString()
        fragmentArguments = parcel.readBundle(javaClass.classLoader)
        intent = parcel.readIntent()
        extras = parcel.readBundle(javaClass.classLoader)
    }

    companion object CREATOR : Parcelable.Creator<Header> {
        override fun createFromParcel(source: Parcel): Header =
            Header(source)

        override fun newArray(size: Int): Array<Header?> =
            arrayOfNulls(size)

        private fun Parcel.writeCharSequence(cs: CharSequence?, flags: Int): Unit =
            TextUtils.writeToParcel(cs, this, flags)

        private fun Parcel.readCharSequence(): CharSequence? =
            TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(this)

        private fun Parcel.writeIntent(intent: Intent?, flags: Int): Unit = intent?.let {
            writeInt(1)
            it.writeToParcel(this, flags)
        } ?: run { writeInt(0) }

        private fun Parcel.readIntent(): Intent? =
            if (readInt() != 0) Intent.CREATOR.createFromParcel(this)
            else null
    }
}
