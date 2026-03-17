package com.example.persistenttimerapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.persistenttimerapp.databinding.ActivityMainBinding
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var dataHelper: DataHelper

    private val timer = Timer()

    // The total countdown duration in milliseconds (set by user input)
    private var countdownDurationMs: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dataHelper = DataHelper(applicationContext)

        binding.startButton.setOnClickListener { startStopAction() }
        binding.resetButton.setOnClickListener { resetAction() }

        // Restore saved countdown duration
        countdownDurationMs = dataHelper.getCountdownDuration()

        if (dataHelper.timerCounting()) {
            // Timer is actively running — hide input, show countdown
            showTimerMode()
            startTimer()
        } else if (dataHelper.startTime() != null && dataHelper.stopTime() != null) {
            // Timer was paused — show remaining time
            showTimerMode()
            stopTimer()
            val elapsed = dataHelper.stopTime()!!.time - dataHelper.startTime()!!.time
            val remaining = countdownDurationMs - elapsed
            binding.timeTV.text = timeStringFromLong(maxOf(remaining, 0))
        } else {
            // No timer active — show input fields
            showInputMode()
            binding.timeTV.text = timeStringFromLong(0)
        }

        timer.scheduleAtFixedRate(TimeTask(), 0, 500)
    }

    private inner class TimeTask : TimerTask() {
        override fun run() {
            if (dataHelper.timerCounting()) {
                val elapsed = Date().time - dataHelper.startTime()!!.time
                val remaining = countdownDurationMs - elapsed

                if (remaining <= 0) {
                    // Countdown finished
                    runOnUiThread {
                        binding.timeTV.text = timeStringFromLong(0)
                        resetAction()
                        // TODO: trigger an alarm/notification here if you want
                    }
                } else {
                    runOnUiThread {
                        binding.timeTV.text = timeStringFromLong(remaining)
                    }
                }
            }
        }
    }

    private fun resetAction() {
        dataHelper.setStopTime(null)
        dataHelper.setStartTime(null)
        dataHelper.setCountdownDuration(0)
        countdownDurationMs = 0
        stopTimer()
        binding.timeTV.text = timeStringFromLong(0)
        showInputMode()
    }

    private fun showInputMode() {
        binding.inputHours.visibility = View.VISIBLE
        binding.inputMinutes.visibility = View.VISIBLE
        binding.inputSeconds.visibility = View.VISIBLE
        binding.inputLabel.visibility = View.VISIBLE
    }

    private fun showTimerMode() {
        binding.inputHours.visibility = View.GONE
        binding.inputMinutes.visibility = View.GONE
        binding.inputSeconds.visibility = View.GONE
        binding.inputLabel.visibility = View.GONE
    }

    private fun getInputDurationMs(): Long {
        val h = binding.inputHours.text.toString().toLongOrNull() ?: 0
        val m = binding.inputMinutes.text.toString().toLongOrNull() ?: 0
        val s = binding.inputSeconds.text.toString().toLongOrNull() ?: 0
        return (h * 3600 + m * 60 + s) * 1000
    }

    private fun timeStringFromLong(ms: Long): String {
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = (totalSeconds / 3600) % 24
        return makeTimeString(hours, minutes, seconds)
    }

    private fun makeTimeString(hours: Long, minutes: Long, seconds: Long): String {
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun stopTimer() {
        dataHelper.setTimerCounting(false)
        binding.startButton.text = getString(R.string.start)
    }

    private fun startTimer() {
        dataHelper.setTimerCounting(true)
        binding.startButton.text = getString(R.string.stop)
    }

    private fun startStopAction() {
        if (dataHelper.timerCounting()) {
            // Pause: record when we stopped
            dataHelper.setStopTime(Date())
            stopTimer()
        } else {
            if (dataHelper.stopTime() != null) {
                // Resume from pause: adjust start time to account for paused duration
                val pausedDuration = Date().time - dataHelper.stopTime()!!.time
                dataHelper.setStartTime(Date(dataHelper.startTime()!!.time + pausedDuration))
                dataHelper.setStopTime(null)
            } else {
                // Fresh start: read duration from input fields
                countdownDurationMs = getInputDurationMs()
                if (countdownDurationMs <= 0) return // Don't start with 0
                dataHelper.setCountdownDuration(countdownDurationMs)
                dataHelper.setStartTime(Date())
            }
            showTimerMode()
            startTimer()
        }
    }
}