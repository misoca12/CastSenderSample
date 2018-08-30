package com.misoca.castsendersample

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.widget.*
import com.google.android.gms.cast.framework.*
import com.google.android.gms.common.images.WebImage
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaMetadata.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaStatus
import org.json.JSONObject
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.media.MediaQueue
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    lateinit var castContext: CastContext
    var castSession: CastSession? = null
    lateinit var sessionManager: SessionManager
    val sessionManagerListener = SessionManagerListenerImpl()
    var remoteMediaClient :RemoteMediaClient? = null

    private val callback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            if (recyclerView.adapter != null) return
            val mediaQueue = remoteMediaClient!!.mediaQueue ?: return
            showQueueList(mediaQueue)
            checkMediaStatus()
        }
    }

    private val listener = object : MediaQueueAdapter.MediaQueueAdapterListener {
        override fun onClick(position: Int) {
            if (remoteMediaClient is RemoteMediaClient) {
                val mediaQueueItem = remoteMediaClient!!.mediaQueue.getItemAtIndex(position)
//              val itemId = remoteMediaClient!!.mediaQueue.itemIdAtIndex(position)
                val itemId = mediaQueueItem!!.itemId
                Log.d(TAG, "Jump to position=${position}(${itemId})")
                val result = remoteMediaClient!!.queueJumpToItem(itemId, null)
                remoteMediaClient!!.requestStatus()
                result.setResultCallback {
                    Log.d(TAG, it.toString())
                }
            }
        }
    }

    inner class SessionManagerListenerImpl : SessionManagerListener<Session> {
        override fun onSessionStarting(session: Session?) {
            Log.d(TAG, "onSessionStarting")
        }

        override fun onSessionStartFailed(session: Session?, p1: Int) {
            Log.d(TAG, "onSessionStartFailed")
        }

        override fun onSessionStarted(session: Session, sessionId: String) {
            Log.d(TAG, "onSessionStarted")
            Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
            if (session is CastSession) {
                castSession = session
                remoteMediaClient = castSession!!.remoteMediaClient
                buttonLoad.isEnabled = true
                buttonQueueLoad.isEnabled = true
            }
            if (remoteMediaClient is RemoteMediaClient) {
                val mediaQueue = remoteMediaClient!!.mediaQueue ?: return
                showQueueList(mediaQueue)
            }
        }

        override fun onSessionResuming(session: Session?, p1: String?) {
            Log.d(TAG, "onSessionResuming")
        }

        override fun onSessionResumeFailed(session: Session?, p1: Int) {
            Log.d(TAG, "onSessionResumeFailed")
        }

        override fun onSessionResumed(session: Session, wasSuspended: Boolean) {
            invalidateOptionsMenu()
            if (session is CastSession) {
                castSession = session
                remoteMediaClient = castSession!!.remoteMediaClient
                buttonLoad.isEnabled = true
                buttonQueueLoad.isEnabled = true
            }
            if (remoteMediaClient is RemoteMediaClient) {
                val mediaQueue = remoteMediaClient!!.mediaQueue ?: return
                showQueueList(mediaQueue)
            }
        }

        override fun onSessionSuspended(session: Session?, p1: Int) {
            Log.d(TAG, "onSessionSuspended")
        }

        override fun onSessionEnding(session: Session?) {
            Log.d(TAG, "onSessionEnding")
        }

        override fun onSessionEnded(session: Session, error: Int) {
            Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()
            buttonLoad.isEnabled = false
            buttonQueueLoad.isEnabled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttonLoad.setOnClickListener { view ->
            loadMedia()
        }
        buttonQueueLoad.setOnClickListener { view ->
            loadMediaQueue()
        }
        castContext = CastContext.getSharedInstance(this)
        sessionManager = castContext.sessionManager

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        castSession = sessionManager.currentCastSession
        sessionManager.addSessionManagerListener(sessionManagerListener)
        // MediaRouteボタンを直接配置した場合の初期化
        CastButtonFactory.setUpMediaRouteButton(applicationContext, mediaRouteButton)
    }

    override fun onPause() {
        super.onPause()
        sessionManager.removeSessionManagerListener(sessionManagerListener)
        castSession = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        // MediaRouteボタンをメニューに配置した場合の初期化
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        return true
    }

    private fun loadMedia() {
        if (castSession == null) {
            return
        }

        // カスタムデータ（Json形式でなんでも渡せる）
        val customData = JSONObject("{\"device_id\":\"9999999\"}")

        val mediaMetadata = MediaMetadata(MEDIA_TYPE_MOVIE)
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, "title")
        mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, "sub title")
        mediaMetadata.addImage(WebImage(Uri.parse(IMAGE_URL)))

        val mediaInfo = MediaInfo.Builder(AUDIO_URL)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("audio/mp4")
                .setMetadata(mediaMetadata)
                .setCustomData(customData)
                .setEntity("this is Deeplink")
                .build()

        // 認証情報付与
        val mediaLoadOptions = MediaLoadOptions.Builder()
                .setCredentials("this is Credentials")
                .setCredentialsType("Credentials Type")
                .build()

        remoteMediaClient!!.load(mediaInfo, mediaLoadOptions)

    }

    private fun loadMediaQueue() {
        if (castSession == null) {
            return
        }

        // カスタムデータ（Json形式でなんでも渡せる）
        val customData = JSONObject("{\"content_id\":\"${editTextContentId.text}\"}")

        remoteMediaClient = castSession!!.remoteMediaClient ?: return

        // コールバック設定
        remoteMediaClient!!.registerCallback(callback)

        val queueItems :Array<MediaQueueItem> = Array(5, { it ->
            val mediaMetadata = MediaMetadata(MEDIA_TYPE_MOVIE)
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, "title ${it + 1}")
            mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, "sub title ${it + 1}")
            mediaMetadata.addImage(WebImage(Uri.parse(IMAGE_URL)))

            val mediaInfo = MediaInfo.Builder(AUDIO_URL)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setContentType("audio/mp4")
                    .setMetadata(mediaMetadata)
                    .setCustomData(customData)
                    .setEntity("this is Deeplink?")
                    .build()

            // MediaOptionsは指定できない
            // おそらく複数楽曲を渡している時点で一般的には認証情報が不要だという設計？
            MediaQueueItem.Builder(mediaInfo)
                    .clearItemId()
                    .setAutoplay(true)
                    .setPreloadTime(20.0)
                    .build()
        })

        // 開始曲
        val startIndex = 0
        // リピート種別
        val repeatMode = MediaStatus.REPEAT_MODE_REPEAT_ALL
        // 開始位置
        val playPosition = 200L

        remoteMediaClient!!.queueLoad(queueItems, startIndex, repeatMode, playPosition, customData)

    }

    private fun checkMediaStatus() {
        val mediaStatus = remoteMediaClient!!.mediaStatus
        if (mediaStatus is MediaStatus) {
            Log.d(TAG, "COMMAND_PAUSE         support is ${mediaStatus.isMediaCommandSupported(MediaStatus.COMMAND_PAUSE)}")
            Log.d(TAG, "COMMAND_SEEK          support is ${mediaStatus.isMediaCommandSupported(MediaStatus.COMMAND_SEEK)}")
            Log.d(TAG, "COMMAND_SET_VOLUME    support is ${mediaStatus.isMediaCommandSupported(MediaStatus.COMMAND_SET_VOLUME)}")
            Log.d(TAG, "COMMAND_SKIP_BACKWARD support is ${mediaStatus.isMediaCommandSupported(MediaStatus.COMMAND_SKIP_BACKWARD)}")
            Log.d(TAG, "COMMAND_SKIP_FORWARD  support is ${mediaStatus.isMediaCommandSupported(MediaStatus.COMMAND_SKIP_FORWARD)}")
            Log.d(TAG, "COMMAND_TOGGLE_MUTE   support is ${mediaStatus.isMediaCommandSupported(MediaStatus.COMMAND_TOGGLE_MUTE)}")
            Log.d(TAG, "COMMAND_QUEUE_NEXT    support is ${mediaStatus.isMediaCommandSupported(64L)}")
            Log.d(TAG, "COMMAND_QUEUE_PREV    support is ${mediaStatus.isMediaCommandSupported(128L)}")
        }
    }

    private fun showQueueList(mediaQueue: MediaQueue) {
        for ((index, itemId) in mediaQueue.itemIds.withIndex()){
            Log.d(TAG, "index=${index} itemId=${itemId}")
        }
        recyclerView.adapter = MediaQueueAdapter(this, mediaQueue, listener)
        recyclerView.adapter.notifyDataSetChanged()
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
        const val AUDIO_URL = "https://raw.githubusercontent.com/misoca12/misoca12.github.io/master/GoogleCastReceiver/v3/mix_03.mp3"
        const val IMAGE_URL = "https://raw.githubusercontent.com/misoca12/misoca12.github.io/master/GoogleCastReceiver/v3/my.png"
    }
}
