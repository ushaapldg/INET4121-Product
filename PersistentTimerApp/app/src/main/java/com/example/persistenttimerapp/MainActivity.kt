package com.example.persistenttimerapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.persistenttimerapp.databinding.ActivityMainBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

/**
 * Main Activity that manages both the Persistent Timer and the Calendar View.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dataHelper: DataHelper

    // Timer Variables
    private val timer = Timer()
    private var countdownDurationMs: Long = 0

    // Calendar Variables
    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()

    /**
     * Initializes the activity, sets up view binding, and triggers setup for timer and calendar.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataHelper = DataHelper(applicationContext)

        setupTimer()
        setupCalendar()
    }

    // --- Timer Logic ---

    /**
     * Configures the timer UI, restores saved state from DataHelper, and starts the background update task.
     */
    private fun setupTimer() {
        binding.startButton.setOnClickListener { startStopAction() }
        binding.resetButton.setOnClickListener { resetAction() }

        countdownDurationMs = dataHelper.getCountdownDuration()

        if (dataHelper.timerCounting()) {
            showTimerMode()
            startTimer()
        } else if (dataHelper.startTime() != null && dataHelper.stopTime() != null) {
            showTimerMode()
            stopTimer()
            val elapsed = dataHelper.stopTime()!!.time - dataHelper.startTime()!!.time
            val remaining = countdownDurationMs - elapsed
            binding.timeTV.text = timeStringFromLong(maxOf(remaining, 0))
        } else {
            showInputMode()
            binding.timeTV.text = timeStringFromLong(0)
        }

        timer.scheduleAtFixedRate(TimeTask(), 0, 500)
    }

    /**
     * Background task that calculates remaining time and updates the UI on the main thread.
     */
    private inner class TimeTask : TimerTask() {
        override fun run() {
            if (dataHelper.timerCounting()) {
                val elapsed = Date().time - dataHelper.startTime()!!.time
                val remaining = countdownDurationMs - elapsed

                runOnUiThread {
                    if (remaining <= 0) {
                        binding.timeTV.text = timeStringFromLong(0)
                        resetAction()
                    } else {
                        binding.timeTV.text = timeStringFromLong(remaining)
                    }
                }
            }
        }
    }

    /**
     * Resets the timer state in both memory and persistent storage, clearing inputs and stopping the countdown.
     */
    private fun resetAction() {
        dataHelper.setStopTime(null)
        dataHelper.setStartTime(null)
        dataHelper.setCountdownDuration(0)
        countdownDurationMs = 0
        stopTimer()
        binding.timeTV.text = timeStringFromLong(0)
        showInputMode()
    }

    /**
     * UI helper to show the input fields (HH:MM:SS) when the timer is not active.
     */
    private fun showInputMode() {
        binding.inputHours.visibility = View.VISIBLE
        binding.inputMinutes.visibility = View.VISIBLE
        binding.inputSeconds.visibility = View.VISIBLE
        binding.inputLabel.visibility = View.VISIBLE
    }

    /**
     * UI helper to hide the input fields while the timer is running or paused.
     */
    private fun showTimerMode() {
        binding.inputHours.visibility = View.GONE
        binding.inputMinutes.visibility = View.GONE
        binding.inputSeconds.visibility = View.GONE
        binding.inputLabel.visibility = View.GONE
    }

    /**
     * Reads the current values from the input fields and converts them to total milliseconds.
     */
    private fun getInputDurationMs(): Long {
        val h = binding.inputHours.text.toString().toLongOrNull() ?: 0
        val m = binding.inputMinutes.text.toString().toLongOrNull() ?: 0
        val s = binding.inputSeconds.text.toString().toLongOrNull() ?: 0
        return (h * 3600 + m * 60 + s) * 1000
    }

    /**
     * Converts a millisecond value into a formatted "HH:MM:SS" string.
     */
    private fun timeStringFromLong(ms: Long): String {
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = (totalSeconds / 3600) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Updates the data helper and UI button text to reflect a stopped/paused state.
     */
    private fun stopTimer() {
        dataHelper.setTimerCounting(false)
        binding.startButton.text = "Start"
    }

    /**
     * Updates the data helper and UI button text to reflect a running state.
     */
    private fun startTimer() {
        dataHelper.setTimerCounting(true)
        binding.startButton.text = "Stop"
    }

    /**
     * Handles logic for the Start/Stop button, managing pause/resume and new timer starts.
     */
    private fun startStopAction() {
        if (dataHelper.timerCounting()) {
            dataHelper.setStopTime(Date())
            stopTimer()
        } else {
            if (dataHelper.stopTime() != null) {
                // Resume from pause: adjust start time by the duration spent paused
                val pausedDuration = Date().time - dataHelper.stopTime()!!.time
                dataHelper.setStartTime(Date(dataHelper.startTime()!!.time + pausedDuration))
                dataHelper.setStopTime(null)
            } else {
                // Start a fresh timer
                countdownDurationMs = getInputDurationMs()
                if (countdownDurationMs <= 0) return
                dataHelper.setCountdownDuration(countdownDurationMs)
                dataHelper.setStartTime(Date())
            }
            showTimerMode()
            startTimer()
        }
    }

    // --- Calendar Logic ---

    /**
     * Configures the CalendarView, sets up day binding (rendering and selection), and navigation listeners.
     */
    private fun setupCalendar() {
        // Binds data and logic to each day cell in the calendar
        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()

                // Selection logic when a day is clicked
                container.view.setOnClickListener {
                    if (data.position == DayPosition.MonthDate) {
                        val currentSelection = selectedDate
                        selectedDate = if (currentSelection == data.date) null else data.date
                        binding.calendarView.notifyDateChanged(data.date)
                        currentSelection?.let { binding.calendarView.notifyDateChanged(it) }
                    }
                }

                // Styling for month dates (Today, Selected, and Regular)
                if (data.position == DayPosition.MonthDate) {
                    container.textView.visibility = View.VISIBLE
                    container.textView.setTypeface(null, Typeface.NORMAL)
                    container.textView.background = null

                    when (data.date) {
                        selectedDate -> {
                            container.textView.setTextColor(Color.WHITE)
                            container.textView.setBackgroundColor(Color.BLUE)
                        }
                        today -> {
                            container.textView.setTextColor(Color.BLUE)
                            container.textView.setTypeface(null, Typeface.BOLD)
                        }
                        else -> {
                            container.textView.setTextColor(Color.BLACK)
                        }
                    }
                } else {
                    // Gray out dates from adjacent months
                    container.textView.setTextColor(Color.LTGRAY)
                }
            }
        }

        // Updates the Month/Year title when the calendar is scrolled
        binding.calendarView.monthScrollListener = { month ->
            val title = "${month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.yearMonth.year}"
            binding.monthYearText.text = title
        }

        // Navigation for Next and Previous month buttons
        binding.nextMonthButton.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.scrollToMonth(it.yearMonth.nextMonth)
            }
        }

        binding.previousMonthButton.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.scrollToMonth(it.yearMonth.previousMonth)
            }
        }

        // Initial setup of the calendar range and starting month
        val currentMonth = YearMonth.now()
        binding.calendarView.setup(currentMonth.minusMonths(100), currentMonth.plusMonths(100), daysOfWeek().first())
        binding.calendarView.scrollToMonth(currentMonth)
    }

    /**
     * View holder for a single calendar day cell.
     */
    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: android.widget.TextView = view.findViewById(R.id.calendarDayText)
    }
}
