package io.github.dharmin007.calendar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.setPadding
import com.google.android.material.color.MaterialColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * A month-view calendar widget: month/year header with previous/next arrows and a
 * 7x6 day grid. Days can be tapped; dates carrying events get a dot marker.
 *
 * Events come from an [EventProvider] supplied by the host app, so this view never
 * touches storage itself. Day clicks are delivered to [onDayClickListener].
 *
 * XML attributes (see `MonthView` styleable):
 * - `app:firstDayOfWeek` — monday..sunday (default sunday)
 * - `app:showAdjacentMonthDays` — dimmed trailing/leading days (default true)
 * - `app:animateMonthChange` — slide/fade between months (default true)
 */
class MonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    /** Called when the user taps a day cell. */
    fun interface OnDayClickListener {
        fun onDayClick(view: MonthView, date: LocalDate)
    }

    /**
     * Supplies the dates that carry events for a given month. Called on the main
     * thread whenever the displayed month changes or [refreshEvents] is invoked,
     * so keep it fast (cache in the host app if the lookup is expensive).
     */
    fun interface EventProvider {
        fun getEventDates(month: YearMonth): Set<LocalDate>
    }

    var displayedMonth: YearMonth = YearMonth.now()
        private set

    var selectedDate: LocalDate? = null
        private set

    var firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY
        set(value) {
            field = value
            if (viewsReady) render(animate = false)
        }

    var showAdjacentMonthDays: Boolean = true
        set(value) {
            field = value
            if (viewsReady) render(animate = false)
        }

    var animateMonthChange: Boolean = true

    var eventProvider: EventProvider? = null
        set(value) {
            field = value
            if (viewsReady) {
                queryEvents()
                render(animate = false)
            }
        }

    var onDayClickListener: OnDayClickListener? = null

    private var viewsReady = false

    private val titleView: TextView
    private val grid: GridLayout
    private var eventDates: Set<LocalDate> = emptySet()

    private val colorPrimary: Int
    private val colorOnPrimary: Int
    private val dayTextColor: Int
    private val secondaryTextColor: Int
    private val sundayColor = Color.parseColor("#D73C10")
    private val saturdayColor = Color.parseColor("#009EF7")

    private val titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dayFormatter = DateTimeFormatter.ofPattern("d")
    private val a11yFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())

    init {
        orientation = VERTICAL

        val styled = context.obtainStyledAttributes(attrs, R.styleable.MonthView, defStyleAttr, 0)
        val attrFirstDay: DayOfWeek
        val attrAdjacent: Boolean
        val attrAnimate: Boolean
        try {
            attrFirstDay = DayOfWeek.of(styled.getInt(R.styleable.MonthView_firstDayOfWeek, 7))
            attrAdjacent = styled.getBoolean(R.styleable.MonthView_showAdjacentMonthDays, true)
            attrAnimate = styled.getBoolean(R.styleable.MonthView_animateMonthChange, true)
        } finally {
            styled.recycle()
        }
        // Direct field sets: views aren't built yet, so guarded setters just store.
        firstDayOfWeek = attrFirstDay
        showAdjacentMonthDays = attrAdjacent
        animateMonthChange = attrAnimate

        colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
        colorOnPrimary =
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)
        dayTextColor = MaterialColors.getColor(this, android.R.attr.textColorPrimary)
        secondaryTextColor = MaterialColors.getColor(this, android.R.attr.textColorSecondary)

        // Header: <  September 2026  >
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4))
        }
        val prev = navButton(R.drawable.cw_ic_chevron_left, R.string.cw_previous_month) {
            displayedMonth = displayedMonth.minusMonths(1)
            onMonthChanged(forward = false)
        }
        val next = navButton(R.drawable.cw_ic_chevron_right, R.string.cw_next_month) {
            displayedMonth = displayedMonth.plusMonths(1)
            onMonthChanged(forward = true)
        }
        titleView = AppCompatTextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(dayTextColor)
        }
        header.addView(prev)
        header.addView(titleView)
        header.addView(next)
        addView(header)

        grid = GridLayout(context).apply {
            columnCount = 7
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(grid)

        viewsReady = true
        queryEvents()
        render(animate = false)
    }

    // region Public API

    /** Show the month containing [date] and select it. */
    fun goToDate(date: LocalDate) {
        displayedMonth = YearMonth.from(date)
        selectedDate = date
        queryEvents()
        render(animate = false)
    }

    /** Show the current month and select today. */
    fun goToToday() = goToDate(LocalDate.now())

    /** Show [month] without changing the selection. */
    fun showMonth(month: YearMonth, animate: Boolean = true) {
        if (month == displayedMonth) return
        val forward = month > displayedMonth
        displayedMonth = month
        queryEvents()
        render(animate = animate && animateMonthChange, forward = forward)
    }

    /** Convenience for a fixed set of event dates (wraps [eventProvider]). */
    fun setEventDates(dates: Set<LocalDate>) {
        eventProvider = EventProvider { dates }
    }

    /** Re-query [eventProvider] for the displayed month and redraw. */
    fun refreshEvents() {
        queryEvents()
        render(animate = false)
    }

    /** Clear the current selection. */
    fun clearSelection() {
        selectedDate = null
        render(animate = false)
    }

    // endregion

    private fun onMonthChanged(forward: Boolean) {
        queryEvents()
        render(animate = animateMonthChange, forward = forward)
    }

    private fun queryEvents() {
        eventDates = try {
            eventProvider?.getEventDates(displayedMonth).orEmpty()
        } catch (e: Exception) {
            android.util.Log.w("MonthView", "EventProvider threw; showing no events", e)
            emptySet()
        }
    }

    private fun render(animate: Boolean, forward: Boolean = true) {
        titleView.text = displayedMonth.format(titleFormatter)
        grid.removeAllViews()

        val locale = Locale.getDefault()
        val today = LocalDate.now()

        // Weekday header row.
        for (i in 0 until 7) {
            val dow = firstDayOfWeek.plus(i.toLong())
            val label = AppCompatTextView(context).apply {
                text = dow.getDisplayName(TextStyle.SHORT, locale)
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(secondaryTextColor)
                setPadding(dp(4))
            }
            grid.addView(label, cellParams(row = 0, col = i, heightDp = 28))
        }

        val firstOfMonth = displayedMonth.atDay(1)
        val offset = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val firstCellDate = firstOfMonth.minusDays(offset.toLong())

        for (index in 0 until 42) {
            val date = firstCellDate.plusDays(index.toLong())
            val row = index / 7 + 1
            val col = index % 7
            val inMonth = date.month == displayedMonth.month

            if (!inMonth && !showAdjacentMonthDays) {
                grid.addView(View(context), cellParams(row, col, 52))
                continue
            }

            val isToday = date == today
            val isSelected = date == selectedDate
            val hasEvent = eventDates.contains(date)

            val cell = FrameLayout(context).apply {
                isClickable = inMonth
                isFocusable = inMonth
            }
            val dayView = AppCompatTextView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                text = date.format(dayFormatter)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                background = cellBackground(isSelected, isToday)
                setTextColor(
                    when {
                        isSelected -> colorOnPrimary
                        isToday -> colorPrimary
                        !inMonth -> dayTextColor.withAlpha(0.38f)
                        date.dayOfWeek == DayOfWeek.SUNDAY -> sundayColor
                        date.dayOfWeek == DayOfWeek.SATURDAY -> saturdayColor
                        else -> dayTextColor
                    }
                )
                if (isToday && !isSelected) {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                val desc = date.format(a11yFormatter) +
                    (if (isSelected) ". ${context.getString(R.string.cw_selected_date_description)}" else "") +
                    (if (hasEvent) ". Has events" else "")
                contentDescription = desc
            }
            cell.addView(dayView)

            if (hasEvent && inMonth) {
                val dot = View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isSelected) colorOnPrimary else colorPrimary)
                    }
                    val s = dp(6)
                    layoutParams = FrameLayout.LayoutParams(s, s).apply {
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        bottomMargin = dp(7)
                    }
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                cell.addView(dot)
            }

            if (inMonth) {
                cell.setOnClickListener {
                    selectedDate = date
                    onDayClickListener?.onDayClick(this@MonthView, date)
                    render(animate = false)
                }
            } else {
                // Dimmed adjacent-month filler: visible but inert.
                cell.alpha = 0.55f
            }
            grid.addView(cell, cellParams(row, col, 52))
        }

        if (animate) {
            val dir = if (forward) 1 else -1
            grid.translationX = dir * dp(24).toFloat()
            grid.alpha = 0.4f
            grid.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(220)
                .start()
        } else {
            grid.translationX = 0f
            grid.alpha = 1f
        }
    }

    private fun cellBackground(isSelected: Boolean, isToday: Boolean): Drawable {
        // Non-null content keeps the ripple bounded to the cell.
        val content: Drawable = when {
            isSelected -> GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorPrimary)
            }
            isToday -> GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), colorPrimary)
                setColor(Color.TRANSPARENT)
            }
            else -> ColorDrawable(Color.TRANSPARENT)
        }
        return RippleDrawable(
            ColorStateList.valueOf(colorPrimary.withAlpha(0.2f)),
            content,
            null
        )
    }

    private fun cellParams(row: Int, col: Int, heightDp: Int): GridLayout.LayoutParams =
        GridLayout.LayoutParams(
            GridLayout.spec(row, 1, 1f),
            GridLayout.spec(col, 1, 1f)
        ).apply {
            width = 0
            height = dp(heightDp)
            setGravity(Gravity.FILL)
        }

    private fun navButton(iconRes: Int, descRes: Int, onClick: () -> Unit): ImageButton =
        AppCompatImageButton(context).apply {
            setImageResource(iconRes)
            contentDescription = context.getString(descRes)
            val out = TypedValue()
            context.theme.resolveAttribute(
                androidx.appcompat.R.attr.selectableItemBackgroundBorderless, out, true
            )
            @Suppress("DEPRECATION")
            background = context.getDrawable(out.resourceId)
            setColorFilter(dayTextColor)
            setOnClickListener { onClick() }
            val s = dp(48)
            layoutParams = LayoutParams(s, s)
            setPadding(dp(12))
        }

    private fun Int.withAlpha(alpha: Float): Int {
        val a = (Color.alpha(this) * alpha).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()

    // Rotation-safe: keep month + selection.
    override fun onSaveInstanceState(): Parcelable =
        SavedState(super.onSaveInstanceState()).apply {
            month = displayedMonth.toString()
            selected = selectedDate?.toString()
        }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            displayedMonth = state.month?.let(YearMonth::parse) ?: YearMonth.now()
            selectedDate = state.selected?.let(LocalDate::parse)
            queryEvents()
            render(animate = false)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        var month: String? = null
        var selected: String? = null

        constructor(superState: Parcelable?) : super(superState)
        private constructor(parcel: Parcel) : super(parcel) {
            month = parcel.readString()
            selected = parcel.readString()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(month)
            out.writeString(selected)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(p: Parcel) = SavedState(p)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}
