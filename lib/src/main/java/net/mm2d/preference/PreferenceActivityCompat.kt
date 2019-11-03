/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
@SuppressLint("Registered")
open class PreferenceActivityCompat : AppCompatActivity(),
    PreferenceActivityCompatDelegate.Connector,
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var delegate: PreferenceActivityCompatDelegate

    open val selectedItemPosition: Int
        get() = delegate.selectedItemPosition

    open val headers: List<Header>
        get() = delegate.headers

    open val isMultiPane: Boolean
        get() = delegate.isMultiPane

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate = PreferenceActivityCompatDelegate(this, this)
        delegate.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        delegate.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        delegate.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
        delegate.onRestoreInstanceState(state)
    }

    override fun onBackPressed() {
        if (delegate.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    override fun onIsMultiPane(): Boolean = resources.getBoolean(R.bool.mm2d_pac_dual_pane)

    override fun onIsHidingHeaders(): Boolean = delegate.onIsHidingHeaders()

    override fun onBuildHeaders(target: MutableList<Header>) = Unit

    override fun isValidFragment(fragmentName: String?): Boolean =
        if (applicationInfo.targetSdkVersion >= VERSION_CODES.KITKAT) {
            throw RuntimeException(
                "Subclasses of PreferenceActivity must override isValidFragment(String)"
                    + " to verify that the Fragment class is valid! "
                    + javaClass.name
                    + " has not checked if fragment " + fragmentName + " is valid."
            )
        } else {
            true
        }

    open fun hasHeaders(): Boolean = delegate.hasHeaders()

    open fun invalidateHeaders() {
        delegate.invalidateHeaders()
    }

    open fun loadHeadersFromResource(@XmlRes resId: Int, target: MutableList<Header>) {
        delegate.loadHeadersFromResource(resId, target)
    }

    open fun setListFooter(view: View) {
        delegate.setListFooter(view)
    }

    open fun switchToHeader(fragmentName: String, args: Bundle?) {
        delegate.switchToHeader(fragmentName, args)
    }

    open fun switchToHeader(header: Header) {
        delegate.switchToHeader(header)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        delegate.startPreferenceFragment(pref)
        return true
    }

    open fun onBuildStartFragmentIntent(
        fragmentName: String,
        arguments: Bundle?,
        @StringRes titleRes: Int,
        @StringRes shortTitleRes: Int
    ): Intent = Intent(Intent.ACTION_MAIN).also {
        it.setClass(this, javaClass)
        it.putExtra(EXTRA_SHOW_FRAGMENT, fragmentName)
        it.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, arguments)
        it.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, titleRes)
        it.putExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, shortTitleRes)
        it.putExtra(EXTRA_NO_HEADERS, true)
    }

    @JvmOverloads
    open fun startWithFragment(
        fragmentName: String,
        arguments: Bundle?,
        resultTo: Fragment?,
        resultRequestCode: Int,
        @StringRes titleRes: Int = 0,
        @StringRes shortTitleRes: Int = 0
    ) {
        val intent = onBuildStartFragmentIntent(fragmentName, arguments, titleRes, shortTitleRes)
        if (resultTo == null) {
            startActivity(intent)
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode)
        }
    }

    companion object {
        const val EXTRA_SHOW_FRAGMENT = ":android:show_fragment"
        const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":android:show_fragment_args"
        const val EXTRA_SHOW_FRAGMENT_TITLE = ":android:show_fragment_title"
        const val EXTRA_SHOW_FRAGMENT_SHORT_TITLE = ":android:show_fragment_short_title"
        const val EXTRA_NO_HEADERS = ":android:no_headers"
    }
}
