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
 * This is a compatibility library of PreferenceActivity.
 *
 * This class can be used in much the same way as PreferenceActivity.
 * Moreover, the material design is applied,
 * It is also possible to manage PreferenceFragmentCompat instead of PreferenceFragment.
 */
@SuppressLint("Registered")
open class PreferenceActivityCompat : AppCompatActivity(),
    PreferenceActivityCompatDelegate.Connector,
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var delegate: PreferenceActivityCompatDelegate

    /**
     * Get the cursor row ID of the currently selected list item.
     */
    open val selectedItemPosition: Int
        get() = delegate.selectedItemPosition

    open val headers: List<Header>
        get() = delegate.headers

    /**
     * Returns true if this activity is showing multiple panes -- the headers and a preference fragment.
     */
    open val isMultiPane: Boolean
        get() = delegate.isMultiPane

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate = PreferenceActivityCompatDelegate(this, this)
        delegate.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(delegate.onBackPressedCallback)
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

    /**
     * Called to determine if the activity should run in multi-pane mode.
     * The default implementation returns true if the screen is large enough.
     */
    override fun onIsMultiPane(): Boolean = resources.getBoolean(R.bool.mm2d_pac_dual_pane)

    /**
     * Called to determine whether the header list should be hidden.
     * The default implementation returns the value given in [EXTRA_NO_HEADERS] or false if it is not supplied.
     * This is set to false, for example, when the activity is being re-launched to show a particular preference activity.
     */
    override fun onIsHidingHeaders(): Boolean = delegate.onIsHidingHeaders()

    /**
     * Called when the activity needs its list of headers build.
     * By implementing this and adding at least one item to the list,
     * you will cause the activity to run in its modern fragment mode.
     * Note that this function may not always be called; for example,
     * if the activity has been asked to display a particular fragment without the header list,
     * there is no need to build the headers.
     *
     * Typical implementations will use [loadHeadersFromResource] to fill in the list from a resource.
     */
    override fun onBuildHeaders(target: MutableList<Header>) = Unit

    /**
     * Subclasses should override this method and verify that the given fragment is a valid type to be attached to this activity.
     * The default implementation returns true for apps built for android:targetSdkVersion older than Build.VERSION_CODES.KITKAT.
     * For later versions, it will throw an exception.
     */
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

    /**
     * Returns true if this activity is currently showing the header list.
     */
    open fun hasHeaders(): Boolean = delegate.hasHeaders()

    /**
     * Call when you need to change the headers being displayed.
     * Will result in onBuildHeaders() later being called to retrieve the new list.
     */
    open fun invalidateHeaders() {
        delegate.invalidateHeaders()
    }

    /**
     * Parse the given XML file as a header description, adding each parsed Header into the target list.
     */
    open fun loadHeadersFromResource(@XmlRes resId: Int, target: MutableList<Header>) {
        delegate.loadHeadersFromResource(resId, target)
    }

    /**
     * Set a footer that should be shown at the bottom of the header list.
     */
    open fun setListFooter(view: View) {
        delegate.setListFooter(view)
    }

    /**
     * When in two-pane mode, switch the fragment pane to show the given preference fragment.
     */
    open fun switchToHeader(fragmentName: String, args: Bundle?) {
        delegate.switchToHeader(fragmentName, args)
    }

    /**
     * When in two-pane mode, switch to the fragment pane to show the given preference fragment.
     */
    open fun switchToHeader(header: Header) {
        delegate.switchToHeader(header)
    }

    /**
     * Called when the user has clicked on a Preference that has a fragment class name associated with it.
     * The implementation to should instantiate and switch to an instance of the given fragment.
     */
    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        delegate.startPreferenceFragment(pref)
        return true
    }

    /**
     * Called by [startWithFragment] when in single-pane mode,
     * to build an Intent to launch a new activity showing the selected fragment.
     * The default implementation constructs an Intent that re-launches the current activity with
     * the appropriate arguments to display the fragment.
     */
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

    /**
     * Start a new instance of this activity, showing only the given preference fragment.
     * When launched in this mode, the header list will be hidden and
     * the given preference fragment will be instantiated and fill the entire activity.
     */
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
