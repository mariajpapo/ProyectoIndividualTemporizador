package com.jr.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.CountDownTimer
import android.os.health.TimerStat
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.jr.timer.util.NotificationUtil
import com.jr.timer.util.prefUtil

import kotlinx.android.synthetic.main.activity_timer.*
import kotlinx.android.synthetic.main.content_timer.*
import java.util.*

class Timer : AppCompatActivity() {

    companion object {
        fun setAlarm(context: Context, nowseconds: Long, secondsRemaining: Long): Long{
            val wakeUpTime = (nowseconds +secondsRemaining) *1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            prefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        fun removeAlarm(context: Context){
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            prefUtil.setAlarmSetTime(0,context)
        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000
    }


    enum class TimerState{
        Stopped, Paused, Running
    }

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds = 0L
    private var timerState = TimerState.Stopped

    private var secondsRemaining = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)
        setSupportActionBar(toolbar)
        supportActionBar?.setIcon(R.drawable.ic_timer)
        supportActionBar?.title = "     Timer"

        fab_start.setOnClickListener{ v ->
            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }

        fab_pause.setOnClickListener{ v ->
            timer.cancel()
            timerState = Timer.TimerState.Paused
            updateButtons()
        }

        fab_stop.setOnClickListener{ v ->
            timer.cancel()
            onTimerFinished()
        }
    }

    override fun onResume() {
        super.onResume()
        initTimer()

        removeAlarm(this)
        NotificationUtil.hideTimerNotification(this)
    }

    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running){
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
            NotificationUtil.showTimerRunning(this, wakeUpTime)
        }
        else if(timerState == TimerState.Paused){
            NotificationUtil.showTimerPaused(this)
        }
        prefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        prefUtil.setSecondsRemaining(secondsRemaining, this)
        prefUtil.setTimerState(timerState, this)
    }

    private fun initTimer(){
        timerState = prefUtil.getTimerState(this)

        if (timerState == TimerState.Stopped){
            setNewTimerLength()
        }
        else
            setPreviousTimerLength()

        secondsRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
            prefUtil.getSecondsRemaining(this)
        else
            timerLengthSeconds

        val alarmSetTime = prefUtil.getAlarmSetTime(this)

        if(alarmSetTime>0)
            secondsRemaining -= nowSeconds - alarmSetTime

        if(secondsRemaining<=0)
            onTimerFinished()
        else if (timerState == TimerState.Running)
            startTimer()

        updateButtons()
        updateCountDownUI()
    }

        private fun onTimerFinished(){
            timerState = TimerState.Stopped

            setNewTimerLength()

            progress_countdown.progress = 0

            prefUtil.setSecondsRemaining(timerLengthSeconds, this)
            secondsRemaining = timerLengthSeconds

            updateButtons()
            updateCountDownUI()
        }

        private fun startTimer(){
            timerState = TimerState.Running

            timer = object : CountDownTimer(secondsRemaining*1000, 1000) {
                override fun onFinish() = onTimerFinished()

                override fun onTick(millisUntilFinished: Long) {
                    secondsRemaining = millisUntilFinished / 1000
                    updateCountDownUI()
                }
            }.start()
        }

    private fun setNewTimerLength(){
        val lengthInMinutes = prefUtil.getTimerLength(this)
        timerLengthSeconds = (lengthInMinutes * 60L)
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun setPreviousTimerLength(){
        timerLengthSeconds = prefUtil.getPreviousTimerLengthSeconds(this)
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun updateCountDownUI(){
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondStr = secondsInMinuteUntilFinished.toString()
        textview_countdown.text = "$minutesUntilFinished:${
        if (secondStr.length == 2) secondStr
        else "0" + secondStr}"

        progress_countdown.progress = (timerLengthSeconds -secondsRemaining).toInt()
    }

    private fun updateButtons(){
        when (timerState){
            TimerState.Running ->{
                fab_start.isEnabled = false
                fab_pause.isEnabled = true
                fab_stop.isEnabled = true
            }
            TimerState.Stopped ->{
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = false
            }
            TimerState.Paused ->{
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_timer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
