package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class EveryMinuteSchedule extends RecurringSchedule {
    Frequency getFrequency() {
        Frequency.Minutely
    }
}
