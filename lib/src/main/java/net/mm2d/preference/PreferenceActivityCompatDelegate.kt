/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.preference

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.IdRes
import androidx.annotation.XmlRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle.State
import androidx.preference.Preference
import net.mm2d.preference.PreferenceActivityCompat.Companion.EXTRA_NO_HEADERS
import net.mm2d.preference.PreferenceActivityCompat.Companion.EXTRA_SHOW_FRAGMENT
import net.mm2d.preference.PreferenceActivityCompat.Companion.EXTRA_SHOW_FRAGMENT_ARGUMENTS
import net.mm2d.preference.PreferenceActivityCompat.Companion.EXTRA_SHOW_FRAGMENT_TITLE
import java.util.*

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class PreferenceActivityCompatDelegate(
    private val activity: FragmentActivity,
    private val connector: Connector
) {
    private val _headers = ArrayList<Header>()
    private var listAdapter: ListAdapter? = null
    private lateinit var listView: ListView
    private lateinit var listFooter: FrameLayout
    private lateinit var prefsContainer: ViewGroup
    private lateinit var headersContainer: ViewGroup
    private var breadCrumbTitle: TextView? = null
    private var singlePane: Boolean = false
    private var currentHeader: Header? = null
    private val handler = Handler(Looper.getMainLooper())
    private var fragment: Fragment? = null

    private val requestFocus = Runnable { listView.focusableViewAvailable(listView) }
    private val buildHeaders = Runnable {
        _headers.clear()
        connector.onBuildHeaders(_headers)
        (listAdapter as? BaseAdapter)?.notifyDataSetChanged()
        currentHeader?.let { header ->
            findBestMatchingHeader(header, _headers)?.let {
                setSelectedHeader(it)
            }
        }
    }

    private val context: Context
        get() = activity

    private val intent: Intent
        get() = activity.intent

    private val resources: Resources
        get() = activity.resources

    private val isResumed: Boolean
        get() = activity.lifecycle.currentState == State.RESUMED

    private val fragmentManager: FragmentManager
        get() = activity.supportFragmentManager

    val selectedItemPosition: Int
        get() = listView.selectedItemPosition

    val headers: List<Header>
        get() = _headers

    val isMultiPane: Boolean
        get() = !singlePane

    private fun <T : View> findViewById(@IdRes id: Int): T? = activity.findViewById(id)

    fun onCreate(savedInstanceState: Bundle?) {
        activity.setContentView(R.layout.mm2d_pac_content)
        listView = findViewById(R.id.list)!!
        listView.onItemClickListener =
            OnItemClickListener { _, _, position, _ -> onListItemClick(position) }
        handler.post(requestFocus)
        listFooter = findViewById(R.id.list_footer)!!
        prefsContainer = findViewById(R.id.preference_frame)!!
        headersContainer = findViewById(R.id.headers)!!

        singlePane = connector.onIsHidingHeaders() || !connector.onIsMultiPane()
        val breadCrumbSection: View? = findViewById(R.id.breadcrumb_section)
        val breadCrumbTitle: TextView? = findViewById(R.id.bread_crumb_title)
        if (singlePane && breadCrumbSection != null && breadCrumbTitle != null) {
            breadCrumbTitle.visibility = View.GONE
            breadCrumbSection.visibility = View.GONE
        }
        this.breadCrumbTitle = breadCrumbTitle

        val initialFragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)

        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<Header>(HEADERS_TAG)?.let { savedHeaders ->
                _headers.addAll(savedHeaders)
                val headerIndex = savedInstanceState.getInt(CUR_HEADER_TAG, HEADER_ID_UNDEFINED)
                if (headerIndex >= 0 && headerIndex < _headers.size) {
                    setSelectedHeader(_headers[headerIndex])
                } else if (!singlePane) {
                    switchToHeader(onGetInitialHeader())
                }
            } ?: run {
                showBreadCrumbs(activity.title)
            }
        } else {
            if (initialFragmentName == null || !connector.onIsHidingHeaders()) {
                connector.onBuildHeaders(_headers)
            }
            if (initialFragmentName != null) {
                switchToHeader(
                    initialFragmentName,
                    intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                )
            } else if (!singlePane && _headers.size > 0) {
                switchToHeader(onGetInitialHeader())
            }
        }

        if (_headers.size > 0) {
            HeaderAdapter(context, _headers).also { adapter ->
                listAdapter = adapter
                listView.adapter = adapter
            }
            if (!singlePane) {
                listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE
            }
        }

        val initialTitle = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0)
        if (singlePane && initialFragmentName != null && initialTitle != 0) {
            showBreadCrumbs(activity.getText(initialTitle))
        }

        if (singlePane) {
            if (currentHeader != null || initialFragmentName != null) {
                headersContainer.visibility = View.GONE
            } else {
                prefsContainer.visibility = View.GONE
            }
        } else if (_headers.size > 0) {
            currentHeader?.let { setSelectedHeader(it) }
        }
    }

    fun onDestroy() {
        handler.removeCallbacks(buildHeaders)
        handler.removeCallbacks(requestFocus)
    }

    fun onSaveInstanceState(outState: Bundle) {
        if (_headers.size <= 0) return
        outState.putParcelableArrayList(HEADERS_TAG, _headers)
        currentHeader?.let {
            val index = _headers.indexOf(it)
            if (index >= 0) {
                outState.putInt(CUR_HEADER_TAG, index)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRestoreInstanceState(state: Bundle) {
        if (singlePane) return
        currentHeader?.let { setSelectedHeader(it) }
    }

    fun onBackPressed(): Boolean {
        val manager = fragmentManager
        if (currentHeader == null || isMultiPane || manager.backStackEntryCount != 0) {
            return false
        }
        fragment?.let { manager.beginTransaction().remove(it).commitAllowingStateLoss() }
        fragment = null
        currentHeader = null
        prefsContainer.visibility = View.GONE
        headersContainer.visibility = View.VISIBLE
        showBreadCrumbs(activity.title)
        listView.clearChoices()
        return true
    }

    fun hasHeaders(): Boolean = headersContainer.visibility == View.VISIBLE

    fun onIsHidingHeaders(): Boolean = intent.getBooleanExtra(EXTRA_NO_HEADERS, false)

    private fun onGetInitialHeader(): Header = _headers.find { it.fragment != null }
        ?: throw IllegalStateException("Must have at least one header with a fragment")

    fun invalidateHeaders() {
        handler.removeCallbacks(buildHeaders)
        handler.post(buildHeaders)
    }

    fun loadHeadersFromResource(
        @XmlRes resId: Int,
        target: MutableList<Header>
    ) {
        HeaderLoader.loadFromResource(context, resId, target)
    }

    fun setListFooter(view: View) {
        listFooter.removeAllViews()
        listFooter.addView(view, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun onListItemClick(position: Int) {
        if (!isResumed) return
        (listAdapter?.getItem(position) as? Header)?.let { onHeaderClick(it) }
    }

    private fun onHeaderClick(header: Header) {
        if (header.fragment != null) {
            switchToHeader(header)
        } else {
            header.intent?.let { context.startActivity(it) }
        }
    }

    fun switchToHeader(
        fragmentName: String,
        args: Bundle?
    ) {
        _headers.find { fragmentName == it.fragment }?.let {
            setSelectedHeader(it)
        }
        switchToHeaderInner(fragmentName, args)
    }

    fun switchToHeader(header: Header) {
        if (currentHeader == header) {
            fragmentManager.popBackStack(BACK_STACK_PREFS, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } else {
            val fragmentName = header.fragment
                ?: throw IllegalStateException("can't switch to header that has no fragment")
            handler.post {
                switchToHeaderInner(fragmentName, header.fragmentArguments)
                setSelectedHeader(header)
            }
        }
    }

    private fun switchToHeaderInner(
        fragmentName: String,
        fragmentArguments: Bundle?
    ) {
        fragmentManager.popBackStack(BACK_STACK_PREFS, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        require(connector.isValidFragment(fragmentName)) { "Invalid fragment for this activity: $fragmentName" }
        val fragment = instantiateFragment(fragmentName, fragmentArguments)
        this.fragment = fragment
        fragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_NONE)
            .replace(R.id.preference, fragment)
            .commitAllowingStateLoss()

        if (singlePane && prefsContainer.visibility == View.GONE) {
            prefsContainer.visibility = View.VISIBLE
            headersContainer.visibility = View.GONE
        }
    }

    private fun instantiateFragment(
        fragmentName: String,
        fragmentArguments: Bundle?
    ): Fragment {
        val fragment = fragmentManager
            .fragmentFactory
            .instantiate(context.classLoader, fragmentName)
        fragmentArguments?.let {
            it.classLoader = fragment::class.java.classLoader
            fragment.arguments = it
        }
        return fragment
    }

    private fun setSelectedHeader(header: Header) {
        currentHeader = header
        val index = _headers.indexOf(header)
        if (index >= 0) {
            listView.setItemChecked(index, true)
        } else {
            listView.clearChoices()
        }
        showBreadCrumbs(header)
    }

    private fun showBreadCrumbs(header: Header) {
        val title = header.getBreadCrumbTitle(resources)
            ?: header.getTitle(resources)
            ?: activity.title
        showBreadCrumbs(title)
    }

    private fun showBreadCrumbs(title: CharSequence?) {
        val breadCrumbTitle = breadCrumbTitle
        if (breadCrumbTitle?.visibility != View.VISIBLE) {
            activity.title = title
        } else {
            breadCrumbTitle.text = title
        }
    }

    fun startPreferenceFragment(preference: Preference) {
        val fragment = instantiateFragment(preference.fragment, preference.extras)
        fragmentManager.beginTransaction()
            .replace(R.id.preference, fragment)
            .setBreadCrumbTitle(preference.title)
            .setTransition(FragmentTransaction.TRANSIT_NONE)
            .addToBackStack(BACK_STACK_PREFS)
            .commitAllowingStateLoss()
    }

    private fun findBestMatchingHeader(
        current: Header,
        from: ArrayList<Header>
    ): Header? {
        val matches = mutableListOf<Header>()
        from.forEach { oh ->
            if (current == oh || current.id != HEADER_ID_UNDEFINED && current.id == oh.id) {
                return oh
            }
            if (current.fragment != null) {
                if (current.fragment == oh.fragment) {
                    matches.add(oh)
                }
            } else if (current.intent != null) {
                if (current.intent == oh.intent) {
                    matches.add(oh)
                }
            } else if (current.title != null) {
                if (current.title == oh.title) {
                    matches.add(oh)
                }
            }
        }
        if (matches.size == 1) {
            return matches[0]
        }
        matches.forEach { oh ->
            if (current.fragmentArguments?.equals(oh.fragmentArguments) == true) {
                return oh
            }
            if (current.extras?.equals(oh.extras) == true) {
                return oh
            }
            if (current.title?.equals(oh.title) == true) {
                return oh
            }
        }
        return null
    }

    interface Connector {
        fun onBuildHeaders(target: MutableList<Header>)

        fun onIsMultiPane(): Boolean

        fun onIsHidingHeaders(): Boolean

        fun isValidFragment(fragmentName: String?): Boolean
    }

    companion object {
        const val HEADER_ID_UNDEFINED = -1
        private const val HEADERS_TAG = ":android:headers"
        private const val CUR_HEADER_TAG = ":android:cur_header"
        private const val BACK_STACK_PREFS = ":android:prefs"
    }
}
