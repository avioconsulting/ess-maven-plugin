package com.avioconsulting.ess

import com.avioconsulting.ess.builders.ScheduleBuilder
import com.avioconsulting.ess.factories.ScheduleFactory
import com.avioconsulting.ess.models.Direction
import com.avioconsulting.ess.models.RecurringSchedule
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

import java.time.DayOfWeek

class SingleScheduleFactory implements ScheduleFactory {
    RecurringSchedule createSchedule() {
        def holidays = [
                '2017-01-02',
                '2017-02-20',
                '2017-05-29',
                '2017-07-04',
                '2017-09-04',
                '2017-11-23',
                '2017-11-24',
                '2017-12-25'
        ].collect { dateStr -> new LocalDate(dateStr) }

        ScheduleBuilder.getWeeklySchedule name: 'TheSchedule',
                                          displayName: 'the schedule',
                                          description: 'Weekly schedule on mondays',
                                          startDate: new LocalDate(2017, 1, 1),
                                          endDate: new LocalDate(2017, 12, 31),
                                          timeOfDay: new LocalTime(9, 15, 10),
                                          timeZone: DateTimeZone.forID('America/Chicago'),
                                          daysOfWeek: [DayOfWeek.MONDAY],
                                          holidays: holidays,
                                          alternateDirection: Direction.Backward
    }
}
