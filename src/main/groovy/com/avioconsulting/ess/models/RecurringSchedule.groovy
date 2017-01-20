package com.avioconsulting.ess.models

import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime

class RecurringSchedule {
    enum Frequency {
        Weekly
    }

    enum DayOfWeek {
        Monday,
        Tuesday,
        Wednesday,
        Thursday,
        Friday,
        Saturday,
        Sunday
    }

    final String name
    final String description
    final String displayName
    final DateTimeZone timeZone
    final Frequency frequency
    /**
     * Maximum number of times the recurrence is executed
     */
    final int recurrenceCount
    // this is 1 for every week
    final int repeatInterval
    final List<DayOfWeek> daysOfWeek
    final LocalTime timeOfDay
    final Set<LocalDateTime> includeDates
    final Set<LocalDateTime> excludeDates

    RecurringSchedule(String name,
                      String description,
                      String displayName,
                      DateTimeZone timeZone,
                      Frequency frequency,
                      int recurrenceCount,
                      int repeatInterval,
                      List<DayOfWeek> daysOfWeek,
                      LocalTime timeOfDay,
                      Set<LocalDateTime> includeDates,
                      Set<LocalDateTime> excludeDates) {

        this.name = name
        this.description = description
        this.displayName = displayName
        this.timeZone = timeZone
        this.frequency = frequency
        this.recurrenceCount = recurrenceCount
        this.repeatInterval = repeatInterval
        this.daysOfWeek = daysOfWeek
        this.timeOfDay = timeOfDay
        this.includeDates = includeDates
        this.excludeDates = excludeDates
    }
}
