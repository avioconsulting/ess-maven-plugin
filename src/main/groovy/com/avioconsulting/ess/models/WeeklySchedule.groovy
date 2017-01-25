package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

import java.time.DayOfWeek

@Canonical
@InheritConstructors
class WeeklySchedule extends RecurringSchedule {
    List<DayOfWeek> daysOfWeek

    Frequency getFrequency() {
        return Frequency.Weekly
    }
}
