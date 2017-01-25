package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class MonthlySchedule extends RecurringSchedule {
    List<Integer> daysOfMonth

    Frequency getFrequency() {
        Frequency.Monthly
    }
}
