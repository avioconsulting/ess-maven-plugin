package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

@Canonical
@InheritConstructors
class RecurringSchedule extends BaseModel {
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

    String name, description, displayName
    DateTimeZone timeZone
    Frequency frequency

    int recurrenceCount, repeatInterval
    LocalDate startDate, endDate
    List<DayOfWeek> daysOfWeek
    LocalTime timeOfDay
    Set<LocalDate> includeDates, excludeDates

    RecurringSchedule(Map map) {
        super(map << [recurrenceCount: (map.recurrenceCount ?: 0)])
        validateName(this.name)
    }
}
