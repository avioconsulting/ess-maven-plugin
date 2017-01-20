package com.avioconsulting.ess

import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

class RecurringSchedule {
    enum Frequency {
        Weekly
    }

    enum DayOfWeek {
        Sunday,
        Monday,
        Tuesday,
        Wednesday,
        Thursday,
        Friday,
        Saturday
    }

    final String name
    final String description
    final String displayName
    final DateTimeZone timeZone
    final Frequency frequency
    // TODO: This is zero in the XML, what is it???
    final int recurrenceCount
    // this is 1 for every week
    final int repeatInterval
    final List<DayOfWeek> daysOfWeek
    final LocalTime timeOfDay
    // when we generate XML, pair these with the timeOfDay
    final Set<LocalDate> includeDates
    final Set<LocalDate> excludeDates
}
