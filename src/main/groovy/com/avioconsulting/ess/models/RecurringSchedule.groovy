package com.avioconsulting.ess.models

import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

abstract class RecurringSchedule extends BaseModel {
    enum Frequency {
        Weekly,
        Monthly,
        Minutely
    }

    String name, description, displayName
    DateTimeZone timeZone

    // repeatInterval means every 1 week, 2 weeks, etc.
    int recurrenceCount, repeatInterval
    LocalDate startDate, endDate
    LocalTime timeOfDay
    Set<LocalDate> includeDates, excludeDates

    RecurringSchedule(Map map) {
        super(map << [recurrenceCount: (map.recurrenceCount ?: 0)])
        validateName(this.name)
    }

    abstract Frequency getFrequency()
}
