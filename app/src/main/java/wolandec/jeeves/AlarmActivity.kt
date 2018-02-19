package wolandec.jeeves

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_alarm.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var cameraManager: CameraManager? = null
    private var camera: Camera? = null
    private var isFlashOn = false
    private val LOG_TAG = this::class.java.simpleName
    private var params: android.hardware.Camera.Parameters? = null
    private var originalVolume: Int? = null
    private var mAudioManager: AudioManager? = null
    private var startBlinkFlash = false
    private var timer: Timer? = null

    private var blinker: Thread? = null
    val BLINK_DELAY: Long = 500

    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        stopAlarm()
        this.finish()
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_alarm)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreen_content.setOnClickListener { toggle() }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        dummy_button.setOnTouchListener(mDelayHideTouchListener)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
        startAlarm()
    }

    private fun startAlarm() {
//        playAlarm()
//        starBlinkWithFlash()
        startScreenBlink()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this);
        stopAlarm()
    }

    override fun onPause() {
        super.onPause()
        stopAlarm()
    }

    private fun stopAlarm() {
        stopBlinkWithFlash()
        stopScreenBlink()
        stopPlay()
    }

    fun setMaxVolume() {
        mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        originalVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)!!, 0);
    }

    fun playAlarm() {
        val resID = this.getResources().getIdentifier("alarm", "raw", this.packageName)
        mediaPlayer = MediaPlayer.create(this, resID)
        mediaPlayer?.isLooping = true
//        setMaxVolume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer?.setAudioAttributes(AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC).build())
        } else {
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        mediaPlayer?.start()
    }

    fun stopPlay() {
        setOriginalVolume()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun setOriginalVolume() {
        mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume!!, 0);
    }

    fun starBlinkWithFlash() {
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            return
        getCamera(this)
        startBlinkFlash()
    }

    private fun startBlinkFlash() {
        blinker = object : Thread() {
            override fun run() {
                while (!this.isInterrupted) {
                    if (!isFlashOn) {
                        turnOnFlash()
                    } else {
                        turnOffFlash()
                    }
                }
                Thread.sleep(BLINK_DELAY)
            }
        }
        blinker?.start()
    }

    private fun stopScreenBlink() {
        startBlinkFlash = false
        timer?.cancel()
    }

    private fun startScreenBlink() {
        var toggle = false
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (toggle)
                    EventBus.getDefault().post(ScreenChangeEvent(Color.WHITE))
                else
                    EventBus.getDefault().post(ScreenChangeEvent(Color.BLACK))
                toggle = !toggle
            }
        }, 0, BLINK_DELAY)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun turnScreenBlack(screenChangeEvent: ScreenChangeEvent) {
        findViewById<FrameLayout>(R.id.frame).setBackgroundColor(screenChangeEvent.color)
    }

    private fun turnScreenBlack() {
        findViewById<FrameLayout>(R.id.frame).setBackgroundColor(Color.BLACK)
    }

    private fun turnScreenWhite() {
        findViewById<FrameLayout>(R.id.frame).setBackgroundColor(Color.WHITE)
    }

    fun stopBlinkWithFlash() {
        blinker?.interrupt()
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            return
        turnOffFlash()
    }

    private fun getCamera(context: Context) {
        if (cameraManager == null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager = context.getSystemService(CameraManager::class.java)
                } else {
                    params = camera?.getParameters()
                }
            } catch (e: RuntimeException) {
                Log.d(LOG_TAG, e.toString())
            }

        }
    }

    private fun turnOnFlash() {
        if (!isFlashOn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager?.setTorchMode(cameraManager?.getCameraIdList()!![0], true)
            } else {
                if (camera == null || params == null) {
                    return
                }
                params = camera?.getParameters()

                params?.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                camera?.setParameters(params)
                camera?.startPreview()
            }
            isFlashOn = true
        }

    }

    private fun turnOffFlash() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraManager?.setTorchMode(cameraManager?.getCameraIdList()!![0], false)
        } else {
            if (camera == null || params == null) {
                return
            }
            params = camera?.getParameters()
            params?.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF)
            camera?.setParameters(params)
            camera?.stopPreview()
        }
        isFlashOn = false
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
