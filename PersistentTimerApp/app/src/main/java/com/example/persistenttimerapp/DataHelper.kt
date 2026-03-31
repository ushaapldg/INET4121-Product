package com.example.persistenttimerapp

import android.content.Context
import android.content.SharedPreferences
import java.util.*
import java.text.SimpleDateFormat

class DataHelper(context: Context) {
    private var sharedPref : SharedPreferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private var timerCounting = false
    private var startTime: Date? = null
    private var stopTime: Date? = null

    init {
        timerCounting = sharedPref.getBoolean(COUNTING_KEY, false)
        
        sharedPref.getString(START_TIME_KEY, null)?.let {
            startTime = try { dateFormat.parse(it) } catch (e: Exception) { null }
        }

        sharedPref.getString(STOP_TIME_KEY, null)?.let {
            stopTime = try { dateFormat.parse(it) } catch (e: Exception) { null }
        }
    }

    fun startTime(): Date? = startTime

    fun setStartTime(date: Date?) {
        startTime = date
        with(sharedPref.edit()) {
            val stringDate = if (date == null) null else dateFormat.format(date)
            putString(START_TIME_KEY, stringDate)
            apply()
        }
    }

    fun stopTime(): Date? = stopTime

    fun setStopTime(date: Date?) {
        stopTime = date
        with(sharedPref.edit()) {
            val stringDate = if (date == null) null else dateFormat.format(date)
            putString(STOP_TIME_KEY, stringDate)
            apply()
        }
    }
    
    fun timerCounting(): Boolean = timerCounting

    fun setTimerCounting(value: Boolean) {
        timerCounting = value
        with(sharedPref.edit()) {
            putBoolean(COUNTING_KEY, value)
            apply()
        }
    }

    fun setCountdownDuration(ms: Long) {
        with(sharedPref.edit()) {
            putLong(COUNTDOWN_DURATION_KEY, ms)
            apply()
        }
    }

    fun getCountdownDuration(): Long {
        return sharedPref.getLong(COUNTDOWN_DURATION_KEY, 0)
    }

    companion object {
        const val PREFERENCES = "prefs"
        const val START_TIME_KEY = "startKey"
        const val STOP_TIME_KEY = "stopKey"
        const val COUNTING_KEY = "countingkey"
        const val COUNTDOWN_DURATION_KEY = "countdownDuration"
    }
}