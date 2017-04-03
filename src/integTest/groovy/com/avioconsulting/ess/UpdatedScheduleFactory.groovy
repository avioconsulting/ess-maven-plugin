package com.avioconsulting.ess

import com.avioconsulting.ess.factories.ScheduleFactory
import com.avioconsulting.ess.models.MonthlySchedule
import com.avioconsulting.ess.models.RecurringSchedule
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

class UpdatedScheduleFactory implements ScheduleFactory {
    RecurringSchedule createSchedule() {
        new MonthlySchedule(name: 'the_sch_name',
                            description: 'the description',
                            displayName: 'the display name',
                            timeZone: DateTimeZone.forID('America/Chicago'),
                            startDate: new LocalDate(2025, 1, 1),
                            endDate: new LocalDate(2025, 2, 1),
                            repeatInterval: 2,
                            daysOfMonth: [2, 12],
                            timeOfDay: new LocalTime(9, 15, 10),
                            includeDates: [],
                            excludeDates: [])
    }
}
