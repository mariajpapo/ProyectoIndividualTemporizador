package com.jr.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jr.timer.util.NotificationUtil
import com.jr.timer.util.prefUtil

class TimerNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action){
            AppConstants.ACTION_STOP -> {
                Timer.removeAlarm(context)
                prefUtil.setTimerState(Timer.TimerState.Stopped, context)
                NotificationUtil.hideTimerNotification(context)
            }
            AppConstants.ACTION_PAUSE ->{
                var secondsRemaining = prefUtil.getSecondsRemaining(context)
                val alarmSetTime = prefUtil.getAlarmSetTime(context)
                val nowSeconds = Timer.nowSeconds

                secondsRemaining -= nowSeconds - alarmSetTime
                prefUtil.setSecondsRemaining(secondsRemaining, context)

                Timer.removeAlarm(context)
                prefUtil.setTimerState(Timer.TimerState.Paused, context)
                NotificationUtil.showTimerPaused(context)
            }
            AppConstants.ACTION_RESUME -> {
                val secondsRemaining = prefUtil.getSecondsRemaining(context)
                val wakeUpTime = Timer.setAlarm(context, Timer.nowSeconds, secondsRemaining)
                prefUtil.setTimerState(Timer.TimerState.Running, context)
                NotificationUtil.showTimerRunning(context,wakeUpTime)
            }
            AppConstants.ACTION_START -> {
                val minutesRemaining = prefUtil.getTimerLength(context)
                val secondsRemaining = minutesRemaining * 60L
                val wakeUpTime = Timer.setAlarm(context, Timer.nowSeconds, secondsRemaining)
                prefUtil.setTimerState(Timer.TimerState.Running, context)
                prefUtil.setSecondsRemaining(secondsRemaining, context)
                NotificationUtil.showTimerRunning(context,wakeUpTime)
            }
        }
    }
}
