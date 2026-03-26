package com.example.testcalendar

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var selectedDate: LocalDate? = null //variable for the selected date box on calendar
    private val today = LocalDate.now() //variable that indicates the day.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //Values identify the method in activity_main
        //Within the <ImageButton>, it tells kotlin to look for a specific type of view
        //The R.id is an address of the button in the XML file
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val monthYearText = findViewById<TextView>(R.id.monthYearText)
        val nextMonthButton = findViewById<ImageButton>(R.id.nextMonthButton)
        val previousMonthButton = findViewById<ImageButton>(R.id.previousMonthButton)

        // Configure how each day looks
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {

                container.textView.text = data.date.dayOfMonth.toString()

                container.view.setOnClickListener {
                    // Only allow selection of dates in the current month
                    if (data.position == DayPosition.MonthDate) {
                        val currentSelection = selectedDate
                        if (currentSelection == data.date) {
                            selectedDate = null // Deselect
                        } else {
                            selectedDate = data.date
                        }
                        // Refresh both the old and new selections
                        calendarView.notifyDateChanged(data.date)
                        currentSelection?.let { calendarView.notifyDateChanged(it) }
                    }
                }

                // Show dates from the current month, otherwise grey them out
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
                    container.textView.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                }
            }
        }

        // Update the header (Month Year) when scrolling
        calendarView.monthScrollListener = { month ->
            val title = "${month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.yearMonth.year}"
            monthYearText.text = title
        }

        //Select the next month
        nextMonthButton.setOnClickListener {
            //Looks for the current month that is visible
            calendarView.findFirstVisibleMonth()?.let {
                //Extracts the month for the next month
                calendarView.scrollToMonth(it.yearMonth.nextMonth)
            }
        }

        //Selects the previous month
        previousMonthButton.setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let {
                //Extracts the month for the previous month.
                calendarView.scrollToMonth(it.yearMonth.previousMonth)
            }
        }


        // Setup the calendar range
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val firstDayOfWeek = daysOfWeek().first()
        
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)
    }
}

// Container for the views in each day cell
class DayViewContainer(view: View) : ViewContainer(view) {
    val textView: TextView = view.findViewById(R.id.calendarDayText)
}
