package com.github.andreyasadchy.xtra.ui.channel

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.LoggedIn
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.Utils
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerFragment
import com.github.andreyasadchy.xtra.ui.login.LoginActivity
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import com.github.andreyasadchy.xtra.util.*
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.android.synthetic.main.fragment_channel.appBar
import kotlinx.android.synthetic.main.fragment_channel.search
import kotlinx.android.synthetic.main.fragment_channel.toolbar
import kotlinx.android.synthetic.main.fragment_media.*
import kotlinx.android.synthetic.main.fragment_media_pager.*


class ChannelPagerFragment : MediaPagerFragment(), FollowFragment {

    companion object {
        fun newInstance(id: String, login: String, name: String, profileImage: String?) = ChannelPagerFragment().apply {
            bundle.putString(C.CHANNEL_ID, id)
            bundle.putString(C.CHANNEL_LOGIN, login)
            bundle.putString(C.CHANNEL_DISPLAYNAME, name)
            bundle.putString(C.CHANNEL_PROFILEIMAGE, profileImage)
            arguments = bundle
        }
    }

    var bundle = Bundle()
    private val viewModel by viewModels<ChannelPagerViewModel> { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val isLoggedIn = User.get(activity) !is NotLoggedIn
        setAdapter(ChannelPagerAdapter(activity, childFragmentManager, requireArguments()))
        if (activity.isInLandscapeOrientation) {
            appBar.setExpanded(false, false)
        }
        collapsingToolbar.title = requireArguments().getString(C.CHANNEL_DISPLAYNAME)
        logo.loadImage(this, requireArguments().getString(C.CHANNEL_PROFILEIMAGE), circle = true)
        toolbar.apply {
            navigationIcon = Utils.getNavigationIcon(activity)
            setNavigationOnClickListener { activity.popFragment() }
        }
        search.setOnClickListener { activity.openSearch() }
        menu.setOnClickListener { it ->
            PopupMenu(activity, it).apply {
                inflate(R.menu.top_menu)
                menu.findItem(R.id.login).title = if (isLoggedIn) getString(R.string.log_out) else getString(R.string.log_in)
                setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.settings -> { activity.startActivityFromFragment(this@ChannelPagerFragment, Intent(activity, SettingsActivity::class.java), 3) }
                        R.id.login -> {
                            if (!isLoggedIn) {
                                activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 1)
                            } else {
                                AlertDialog.Builder(activity)
                                    .setTitle(getString(R.string.logout_title))
                                    .setMessage(getString(R.string.logout_msg, context?.prefs()?.getString(C.USERNAME, "")))
                                    .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                        activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 2) }
                                    .show()
                            }
                        }
                    }
                    true
                }
                show()
            }
        }
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            private val layoutParams = collapsingToolbar.layoutParams as AppBarLayout.LayoutParams
            private val originalScrollFlags = layoutParams.scrollFlags

            override fun onPageSelected(position: Int) {
//                layoutParams.scrollFlags = if (position != 3) {
                layoutParams.scrollFlags = if (position != 2) {
                    originalScrollFlags
                } else {
                    appBar.setExpanded(false, isResumed)
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        })
    }

    override fun initialize() {
        val activity = requireActivity() as MainActivity
        toolbarContainer.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
        collapsingToolbar.expandedTitleMarginBottom = activity.convertDpToPixels(50.5f)
        if (requireContext().prefs().getBoolean(C.API_USEHELIX, true) && requireContext().prefs().getString(C.USERNAME, "") != "") {
            viewModel.loadStream(requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""), requireContext().prefs().getString(C.TOKEN, "") ?: "", requireArguments().getString(C.CHANNEL_ID) ?: "")
            viewModel.stream.observe(viewLifecycleOwner, Observer {
                if (it.data.firstOrNull() != null) {
                    watchLive.text = getString(R.string.watch_live)
                    it.data.firstOrNull().let { s ->
                        watchLive.setOnClickListener { s?.let { it1 -> activity.startStream(it1) } }
                    }
                } else {
                    watchLive.setOnClickListener { activity.startStream(Stream(user_id = requireArguments().getString(C.CHANNEL_ID) ?: "", user_login = requireArguments().getString(C.CHANNEL_LOGIN) ?: "", user_name = requireArguments().getString(C.CHANNEL_DISPLAYNAME) ?: "", profileImageURL = requireArguments().getString(C.CHANNEL_PROFILEIMAGE) ?: "")) }
                }
            })
        } else {
            watchLive.setOnClickListener { activity.startStream(Stream(user_id = requireArguments().getString(C.CHANNEL_ID) ?: "", user_login = requireArguments().getString(C.CHANNEL_LOGIN) ?: "", user_name = requireArguments().getString(C.CHANNEL_DISPLAYNAME) ?: "", profileImageURL = requireArguments().getString(C.CHANNEL_PROFILEIMAGE) ?: "")) }
        }
        User.get(activity).let {
            if (it is LoggedIn && context?.prefs()?.getBoolean(C.UI_FOLLOW, true) == true) {
                initializeFollow(this, viewModel, follow, it)
            }
        }
    }

    override fun onNetworkRestored() {
        viewModel.retry(requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""), requireContext().prefs().getString(C.TOKEN, "") ?: "")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            appBar.setExpanded(false, false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            requireActivity().recreate()
        }
    }
}