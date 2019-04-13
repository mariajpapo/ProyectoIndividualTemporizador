package com.jr.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jr.timer.util.NotificationUtil
import com.jr.timer.util.prefUtil

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationUtil.showTimerExpired(context)

        prefUtil.setTimerState(Timer.TimerState.Stopped, context)
        prefUtil.setAlarmSetTime(0, context)
    }
}
