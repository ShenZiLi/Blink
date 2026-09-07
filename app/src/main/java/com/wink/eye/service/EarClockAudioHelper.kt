package com.wink.eye.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.wink.eye.data.VibrationMode

/** EarClock 音频辅助：检测耳机连接、将闹铃路由到耳机播放并可选振动 */
object EarClockAudioHelper {
    private const val TAG = "EarClockAudioHelper"

    /** 耳机设备类型（有线 + 蓝牙 A2DP + USB） */
    private val HEADPHONE_DEVICE_TYPES = listOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_USB_HEADSET
    )

    /** 当前是否有耳机连接（仅强依赖有线/USB；蓝牙检测做 best-effort，缺权限属少见情况） */
    fun isHeadphoneConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return try {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .any { it.type in HEADPHONE_DEVICE_TYPES && it.isSink }
        } catch (_: SecurityException) {
            Log.w(TAG, "读取音频输出设备被拒，视为未连接")
            false
        }
    }

    /**
     * 播放闹铃并路由到耳机；返回 MediaPlayer 实例供调用方停止/释放。
     * @param ringtoneUri 自定义铃声 URI，null 时用系统默认闹铃
     * @param vibrationMode 振动模式，OFF 时不振动
     */
    fun playAlarm(
        context: Context,
        ringtoneUri: Uri?,
        vibrationMode: VibrationMode
    ): MediaPlayer {
        val uri = ringtoneUri
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
            // 将闹铃路由到已连接耳机
            findHeadphoneDevice(context)?.let { setPreferredDevice(it) }
            start()
        }
        Log.d(TAG, "开始播放闹铃: $uri")

        if (vibrationMode != VibrationMode.OFF) {
            vibrate(context)
        }
        return player
    }

    /** 从已连接设备中返回第一个可用耳机设备，无则返回 null（不强制路由） */
    private fun findHeadphoneDevice(context: Context): AudioDeviceInfo? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type in HEADPHONE_DEVICE_TYPES && it.isSink }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(longArrayOf(0, 500, 200, 500), 0)
    }
}