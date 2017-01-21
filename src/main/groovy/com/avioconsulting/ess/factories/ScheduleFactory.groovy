package com.avioconsulting.ess.factories

import com.avioconsulting.ess.models.RecurringSchedule

interface ScheduleFactory {
    RecurringSchedule createSchedule()
}