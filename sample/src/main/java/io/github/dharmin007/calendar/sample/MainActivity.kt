package io.github.dharmin007.calendar.sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.dharmin007.calendar.MonthView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val prettyDate =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val monthView = findViewById<MonthView>(R.id.monthView)
        val selectedLabel = findViewById<TextView>(R.id.selectedLabel)

        // Demo events: today + 3, and the 15th of the displayed month.
        monthView.eventProvider = MonthView.EventProvider { month: YearMonth ->
            val today = LocalDate.now()
            buildSet {
                add(today.plusDays(3))
                add(month.atDay(minOf(15, month.lengthOfMonth())))
            }
        }

        monthView.onDayClickListener = MonthView.OnDayClickListener { _, date ->
            selectedLabel.text = date.format(prettyDate)
        }

        findViewById<android.view.View>(R.id.todayButton).setOnClickListener {
            monthView.goToToday()
            selectedLabel.text = LocalDate.now().format(prettyDate)
        }

        findViewById<MaterialSwitch>(R.id.weekStartSwitch).setOnCheckedChangeListener { _, checked ->
            monthView.firstDayOfWeek = if (checked) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        }
    }
}
