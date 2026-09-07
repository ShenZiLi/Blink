package com.wink.eye.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wink.eye.EarClockAlarmActivity
import com.wink.eye.R
import com.wink.eye.WinkApp
import com.wink.eye.data.EarClockAlarm
import com.wink.eye.data.EarClockFrequency

/** EarClock 闹钟触发接收器：校验耳机连接后唤起全屏闹钟页 */
class EarClockAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EarClockAlarmScheduler.EXTRA_ALARM_ID) ?: return
        val isSnooze = intent.getBooleanExtra(EarClockAlarmScheduler.EXTRA_IS_SNOOZE, false)
        val snoozeCount = intent.getIntExtra(EarClockAlarmScheduler.EXTRA_SNOOZE_COUNT, 0)

        Log.d(TAG, "收到闹钟触发: id=$alarmId, isSnooze=$isSnooze, snoozeCount=$snoozeCount")

        val alarm = WinkApp.instance.earClockRepository.getById(alarmId) ?: return
        if (!alarm.enabled) {
            Log.w(TAG, "闹钟已禁用，忽略: $alarmId")
            return
        }

        // 正常触发（非稍后提醒）：WORKDAYS/CUSTOM 重排下一次；ONCE 一次性不重排
        if (!isSnooze && alarm.frequency != EarClockFrequency.ONCE) {
            EarClockAlarmScheduler.scheduleNext(context, alarm)
        }

        // 必须连接耳机才响；未连接时本次静默（已按频率重排下一次）
        if (!EarClockAudioHelper.isHeadphoneConnected(context)) {
            Log.d(TAG, "未连接耳机，本次静默不响: ${alarm.name}")
            return
        }

        launchAlarm(context, alarm, isSnooze, snoozeCount)
    }

    /** 以 fullScreenIntent 高优通知唤起全屏闹钟页（Android 10+ 后台启动限制的可靠路径） */
    private fun launchAlarm(
        context: Context,
        alarm: EarClockAlarm,
        isSnooze: Boolean,
        snoozeCount: Int
    ) {
        ensureChannel(context)

        val fullScreenIntent = Intent(context, EarClockAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EarClockAlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(EarClockAlarmScheduler.EXTRA_IS_SNOOZE, isSnooze)
            putExtra(EarClockAlarmScheduler.EXTRA_SNOOZE_COUNT, snoozeCount)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.earclock_notification_title))
            .setContentText(context.getString(R.string.earclock_notification_text, alarm.name))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(alarm.id.hashCode(), notification)
        Log.d(TAG, "已唤起全屏闹钟页: ${alarm.name}")
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.earclock_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.earclock_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "EarClockAlarmReceiver"
        const val CHANNEL_ID = "wink_earclock"
    }
}