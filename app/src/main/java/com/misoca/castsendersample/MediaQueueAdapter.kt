package com.misoca.castsendersample

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.media.MediaQueue
import com.google.android.gms.cast.framework.media.MediaQueueRecyclerViewAdapter
import kotlinx.android.synthetic.main.item_mediaqueue.view.*

class MediaQueueAdapter(val context: Context, mediaQueue: MediaQueue, val listener: MediaQueueAdapterListener?) : MediaQueueRecyclerViewAdapter<MediaQueueAdapter.MediaQueueViewHolder>(mediaQueue) {

    interface MediaQueueAdapterListener {
        fun onClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MediaQueueViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_mediaqueue, null)
        return MediaQueueViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MediaQueueViewHolder?, position: Int) {
        if (holder is MediaQueueViewHolder) {
            val mediaQueueItem = mediaQueue.getItemAtIndex(position) ?: return
            holder.setMetaData(mediaQueueItem.media.metadata)
            holder.itemView.setOnClickListener {
                if (listener is MediaQueueAdapterListener) {
                    listener.onClick(position)
                }

            }
        }
    }

    inner class MediaQueueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            itemView.setLayoutParams(params)
        }
        fun setMetaData(metadata: MediaMetadata) {
            itemView.title.text = metadata.getString(MediaMetadata.KEY_TITLE)
            itemView.subTitle.text = metadata.getString(MediaMetadata.KEY_SUBTITLE)
        }
    }
}