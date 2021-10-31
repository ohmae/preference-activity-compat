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
 * Description of a single Header item that the user can select.
 */
class Header : Parcelable {
    /**
     * Identifier for this header, to correlate with a new list when it is updated.
     */
    var id = HEADER_ID_UNDEFINED
    /**
     * Resource ID of title of the header that is shown to the user.
     */
    @StringRes
    var titleRes: Int = 0
    /**
     * Title of the header that is shown to the user.
     */
    var title: CharSequence? = null
    /**
     * Resource ID of optional summary describing what this header controls.
     */
    @StringRes
    var summaryRes: Int = 0
    /**
     * Optional summary describing what this header controls.
     */
    var summary: CharSequence? = null
    /**
     * Resource ID of optional text to show as the title in the bread crumb.
     */
    @StringRes
    var breadCrumbTitleRes: Int = 0
    /**
     * Optional text to show as the title in the bread crumb.
     */
    var breadCrumbTitle: CharSequence? = null
    /**
     * Optional icon resource to show for this header.
     */
    var iconRes: Int = 0
    /**
     * Full class name of the fragment to display when this header is selected.
     */
    var fragment: String? = null
    /**
     * Optional arguments to supply to the fragment when it is instantiated.
     */
    var fragmentArguments: Bundle? = null
    /**
     * Intent to launch when the preference is selected.
     */
    var intent: Intent? = null
    /**
     * Optional additional data for use by subclasses of PreferenceActivityCompat.
     */
    var extras: Bundle? = null

    constructor()

    private constructor(parcel: Parcel) {
        readFromParcel(parcel)
    }

    /**
     * Return the currently set title.
     */
    fun getTitle(res: Resources): CharSequence? =
        getText(res, titleRes, title)

    /**
     * Return the currently set summary.
     */
    fun getSummary(res: Resources): CharSequence? =
        getText(res, summaryRes, summary)

    /**
     * Return the currently set bread crumb title.
     */
    fun getBreadCrumbTitle(res: Resources): CharSequence? =
        getText(res, breadCrumbTitleRes, breadCrumbTitle)

    private fun getText(res: Resources, resId: Int, text: CharSequence?): CharSequence? =
        if (resId != 0) res.getText(resId) else text

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.also {
            it.writeInt(id)
            it.writeInt(titleRes)
            it.writeCharSequence(title, flags)
            it.writeInt(summaryRes)
            it.writeCharSequence(summary, flags)
            it.writeInt(breadCrumbTitleRes)
            it.writeCharSequence(breadCrumbTitle, flags)
            it.writeInt(iconRes)
            it.writeString(fragment)
            it.writeBundle(fragmentArguments)
            it.writeIntent(intent, flags)
            it.writeBundle(extras)
        }
    }

    private fun readFromParcel(parcel: Parcel) {
        parcel.let {
            id = it.readInt()
            titleRes = it.readInt()
            title = it.readCharSequence()
            summaryRes = it.readInt()
            summary = it.readCharSequence()
            breadCrumbTitleRes = it.readInt()
            breadCrumbTitle = it.readCharSequence()
            iconRes = it.readInt()
            fragment = it.readString()
            fragmentArguments = it.readBundle(javaClass.classLoader)
            intent = it.readIntent()
            extras = it.readBundle(javaClass.classLoader)
        }
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

        private fun Parcel.writeIntent(intent: Intent?, flags: Int) {
            if (intent != null) {
                writeInt(1)
                intent.writeToParcel(this, flags)
            } else {
                writeInt(0)
            }
        }

        private fun Parcel.readIntent(): Intent? =
            if (readInt() != 0) Intent.CREATOR.createFromParcel(this)
            else null
    }
}
