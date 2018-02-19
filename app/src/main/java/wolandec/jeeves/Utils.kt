package wolandec.jeeves

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*


/**
 * Created by wolandec on 12.02.18.
 */

class Utils {

    companion object {
        private var mediaPlayer: MediaPlayer? = null
        private var cameraManager: CameraManager? = null
        private var camera: Camera? = null
        private var isFlashOn = false
        private val LOG_TAG = this::class.java.simpleName
        private var params: android.hardware.Camera.Parameters? = null

        private var blinker: Thread? = null
        val BLINK_DELAY: Long = 500

        fun setFlagStartedAtBootToTrue(context: Context?) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val sharedPrefEditor: SharedPreferences.Editor = sharedPref.edit()
            sharedPrefEditor.putBoolean("started_at_boot", true)
            sharedPrefEditor.commit()
        }

        fun setMIUIPermsAreCheckedToTrue(context: Context?) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val sharedPrefEditor: SharedPreferences.Editor = sharedPref.edit()
            sharedPrefEditor.putBoolean("miui_perms_are_checked", true)
            sharedPrefEditor.commit()
        }

        fun isMIUI(): Boolean {
            val device = Build.MANUFACTURER;
            if (device.equals("Xiaomi")) {
                try {
                    val prop = Properties();
                    prop.load(FileInputStream(File(Environment.getRootDirectory(), "build.prop")));
                    return prop.getProperty("ro.miui.ui.version.code", null) != null
                            || prop.getProperty("ro.miui.ui.version.name", null) != null
                            || prop.getProperty("ro.miui.internal.storage", null) != null;
                } catch (e: IOException) {
                    e.printStackTrace();
                }

            }
            return false;
        }

        fun getMIUIVersion(): String {
            val device = Build.MANUFACTURER;
            if (device.equals("Xiaomi")) {
                try {
                    val prop = Properties();
                    prop.load(FileInputStream(File(Environment.getRootDirectory(), "build.prop")));
                    return prop.getProperty("ro.miui.ui.version.name", null)
                } catch (e: IOException) {
                    Log.d(LOG_TAG, e.toString())
                    return ""
                }
            }
            return ""
        }

        fun transliterate(message: String): String {
            val abcCyr = charArrayOf(' ', 'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я', 'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z')
            val abcLat = arrayOf(" ", "a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "y", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f", "h", "ts", "ch", "sh", "sch", "", "i", "", "e", "ju", "ja", "A", "B", "V", "G", "D", "E", "E", "Zh", "Z", "I", "Y", "K", "L", "M", "N", "O", "P", "R", "S", "T", "U", "F", "H", "Ts", "Ch", "Sh", "Sch", "", "I", "", "E", "Ju", "Ja", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
            val builder = StringBuilder()
            for (i in 0 until message.length) {
                var specialSymbol = true;
                for (x in abcCyr.indices) {
                    if (message[i] == abcCyr[x]) {
                        builder.append(abcLat[x])
                        specialSymbol = false
                    }
                }
                if (specialSymbol)
                    builder.append(message[i])
            }
            return builder.toString()
        }

        @SuppressLint("NewApi")
        fun getNotification(context: Context): Notification? {

            val notiManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            var builder: NotificationCompat.Builder
            var intent: Intent
            var pendingIntent: PendingIntent

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_LOW;
                val channelId = context.packageName + "_14"
                val androidChannel = NotificationChannel(channelId,
                        context.packageName, importance);
                notiManager.createNotificationChannel(androidChannel);
                var mChannel = notiManager.getNotificationChannel(channelId) as NotificationChannel
                if (mChannel == null) {
                    mChannel = NotificationChannel(channelId, context.packageName, importance)
                    mChannel.setDescription(context.getString(R.string.ready_to_work))
                    mChannel.enableVibration(false)
                    mChannel.setSound(null, null)
                }

                builder = NotificationCompat.Builder(context, channelId)

                intent = Intent(context, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                builder.setContentTitle(context.getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_icon)
                        .setContentText(context.getString(R.string.ready_to_work))
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setTicker("${context.getString(R.string.app_name)}: ${context.getString(R.string.ready_to_work)}")
            } else {
                builder = NotificationCompat.Builder(context, "wolandec.jeeves")
                        .setDefaults(0)
                        .setSmallIcon(R.mipmap.ic_icon)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(R.string.ready_to_work))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(PendingIntent.getActivity(context,
                                1,
                                Intent(context, SettingsActivity::class.java), 0))
            }
            return builder.build();
        }

        fun prepareMessageLength(msgString: String): String {
            var message = msgString
            if (message.length > 70)
                message = Utils.transliterate(message)
            if (message.length > 140)
                message = message.substring(0, 140)
            return message
        }

        fun playAlarm(context: Context) {
            val resID = context.getResources().getIdentifier("alarm", "raw", context.packageName)
            mediaPlayer = MediaPlayer.create(context, resID)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        fun stopAlarm() {
            mediaPlayer?.stop()
        }

        fun starBlinkWithFlash(context: Context) {
            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
                return

            getCamera(context)
            blink()
        }

        private fun blink() {
            intent = Intent(AlarmActivity::class.java)
            blinker = object : Thread() {
                override fun run() {
                    while (!this.isInterrupted) {
                        if (!isFlashOn) {
                            turnScreenWhite()
                            turnOnFlash()
                        } else {
                            turnScreenBlack()
                            turnOffFlash()
                        }
                        Thread.sleep(BLINK_DELAY)
                    }
                }
            }
            blinker?.start()
        }

        private fun turnScreenBlack() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        private fun turnScreenWhite() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        fun stopBlinkWithFlash(context: Context) {
            blinker?.interrupt()
            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
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
                    Log.d(LOG_TAG, e.toString());
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

    }

}