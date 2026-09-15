# Android Calendar Widget

A month-view calendar widget for Android apps. Drop it in a layout, get month
navigation, day selection, and event markers — with no storage or networking
opinions of its own.

This is a ground-up modernization of the original 2011 Eclipse project:
Gradle + Kotlin, `java.time` date math, Material styling, and a clean
`EventProvider` / `OnDayClickListener` API. See [What changed](#what-changed)
for the bug fixes.

## Usage

**Layout:**

```xml
<io.github.dharmin007.calendar.MonthView
    android:id="@+id/monthView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:firstDayOfWeek="monday"
    app:showAdjacentMonthDays="true" />
```

**Code:**

```kotlin
val monthView = findViewById<MonthView>(R.id.monthView)

// Mark dates that have events (query your own DB / API here)
monthView.eventProvider = MonthView.EventProvider { month: YearMonth ->
    myEventDao.datesIn(month) // Set<LocalDate>
}
// ...or a fixed set:
monthView.setEventDates(setOf(LocalDate.now().plusDays(2)))

monthView.onDayClickListener = MonthView.OnDayClickListener { _, date ->
    // date is a java.time.LocalDate
}

monthView.goToDate(someDate)
monthView.goToToday()
monthView.showMonth(YearMonth.now().plusMonths(1))
monthView.clearSelection()
```

The `sample/` module is a runnable demo of all of the above.

### Adding it to your project

The library has no publishing setup of its own; the zero-config route is
[JitPack](https://jitpack.io/): `implementation("com.github.dharmin007:Android-Calendar-Widget:<release-tag>")`.

- `minSdk 21`, `compileSdk 34`, Java 17, `java.time` via core library desugaring
- Dependencies: `androidx.appcompat`, `com.google.android.material`
- Package `io.github.dharmin007.calendar` (Kotlin-safe; the old `in.*`-style roots
  are a reserved word in Kotlin)

## What changed

Modernization and bug fixes vs. the 2011 original:

- **Eclipse → Gradle.** Standard `calendarwidget` (library) + `sample` (demo app)
  modules. This also resolves the old "R cannot be resolved" setup pain (#2):
  there is nothing to hand-wire anymore.
- **Events no longer read a hard-coded private SQLite DB.** The old
  `MonthView` queried `mycal.db` with an app-specific `_schoolid` schema on
  every render; that is replaced by `EventProvider`, which the host app
  implements however it stores events.
- **Last-day-of-month events are marked correctly.** The old range query ended
  at midnight at the *start* of the last day, so events later that day never
  got a marker. Date-only comparison now.
- **Selection highlight rewritten.** The old code kept a `TextView` reference
  across re-renders and restyled a detached (invisible) view; selection is now
  a single `selectedDate: LocalDate?` state with a full re-render.
- **"Today" highlight fixed for `goToDate()`.** `Calendar.equals()` compared
  time-of-day too, so today was never highlighted when the passed `Date` had
  one. Now date-only.
- **Locale-aware names.** Hard-coded English day/month strings are gone;
  month title, weekday labels, and accessibility descriptions use
  `java.time` formatting in the device locale.
- **Stable grid.** Months rendered 4–6 rows depending on the month, making
  layouts jump; the grid is now a fixed 6 rows.
- **Deprecated APIs removed**: `FILL_PARENT`, `Html.fromHtml(String)`,
  `Date.getDate()`, `Calendar` arithmetic → `MATCH_PARENT`, plain text,
  `java.time`.
- **Accessibility**: content descriptions on the month arrows and every day
  cell; selection and event presence are announced.
- **Rotation-safe**: displayed month + selection survive configuration
  changes via saved state.
- **Build outputs removed**: the checked-in `bin/` and `gen/` directories are
  gone; `.gitignore` covers them going forward.

`DBHelper` was deleted with the old storage coupling — if you relied on its
`mycal.db` schema, keep a copy from git history.

## Demo APK

The `sample/` module builds a demo APK (`./gradlew :sample:assembleDebug`).
Releases of this repo can attach that APK for devices without Play Store
access (see issue #1).

## Background

Original write-up (2011):
http://dharmin007.wordpress.com/android-calendar-widget-month-view/
