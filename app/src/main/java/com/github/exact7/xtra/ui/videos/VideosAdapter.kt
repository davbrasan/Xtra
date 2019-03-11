package com.github.exact7.xtra.ui.videos

import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import com.github.exact7.xtra.R
import com.github.exact7.xtra.databinding.FragmentVideosListItemBinding
import com.github.exact7.xtra.model.kraken.video.Video
import com.github.exact7.xtra.ui.common.DataBoundPagedListAdapter
import com.github.exact7.xtra.ui.download.VideoDownloadDialog
import com.github.exact7.xtra.ui.main.MainActivity
import com.github.exact7.xtra.util.DownloadUtils

class VideosAdapter(
        private val mainActivity: MainActivity) : DataBoundPagedListAdapter<Video, FragmentVideosListItemBinding>(
        object : DiffUtil.ItemCallback<Video>() {
            override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean =
                    oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean =
                    oldItem.views == newItem.views &&
                            oldItem.preview == newItem.preview &&
                            oldItem.title == newItem.title
        }) {

    lateinit var lastSelectedItem: Video
        private set

    override val itemId: Int = R.layout.fragment_videos_list_item

    override fun bind(binding: FragmentVideosListItemBinding, item: Video?) {
        binding.video = item
        binding.videoListener = mainActivity
        binding.channelListener = mainActivity
        val activity = binding.root.context as MainActivity
        val showDialog = {
            lastSelectedItem = item!!
            if (DownloadUtils.hasInternalStoragePermission(activity)) {
                VideoDownloadDialog.newInstance(video = item).show(activity.supportFragmentManager, null)
            }
        }
        binding.options.setOnClickListener {
            PopupMenu(activity, binding.options).apply {
                inflate(R.menu.media_item)
                setOnMenuItemClickListener {
                    showDialog.invoke()
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
        binding.root.setOnLongClickListener {
            showDialog.invoke()
            true
        }
    }
}