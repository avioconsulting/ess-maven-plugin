package com.avioconsulting.ess.mappers

import com.avioconsulting.ess.models.RecurringSchedule
import oracle.as.scheduler.ExplicitDate
import oracle.as.scheduler.Recurrence
import oracle.as.scheduler.RecurrenceFields
import oracle.as.scheduler.Schedule
import org.joda.time.LocalDate
import org.joda.time.LocalTime

class ScheduleMapper {
    private static final Map<RecurringSchedule.Frequency, RecurrenceFields.FREQUENCY> frequencyMapping = [
            (RecurringSchedule.Frequency.Weekly): RecurrenceFields.FREQUENCY.WEEKLY
    ]

    private static final Map<RecurringSchedule.DayOfWeek, RecurrenceFields.DAY_OF_WEEK> dayOfWeekMapping = [
            (RecurringSchedule.DayOfWeek.Monday)   : RecurrenceFields.DAY_OF_WEEK.MONDAY,
            (RecurringSchedule.DayOfWeek.Tuesday)  : RecurrenceFields.DAY_OF_WEEK.TUESDAY,
            (RecurringSchedule.DayOfWeek.Wednesday): RecurrenceFields.DAY_OF_WEEK.WEDNESDAY,
            (RecurringSchedule.DayOfWeek.Thursday) : RecurrenceFields.DAY_OF_WEEK.THURSDAY,
            (RecurringSchedule.DayOfWeek.Friday)   : RecurrenceFields.DAY_OF_WEEK.FRIDAY,
            (RecurringSchedule.DayOfWeek.Saturday) : RecurrenceFields.DAY_OF_WEEK.SATURDAY,
            (RecurringSchedule.DayOfWeek.Sunday)   : RecurrenceFields.DAY_OF_WEEK.SUNDAY
    ]

    private static RecurrenceFields.TIME_OF_DAY getTime(LocalTime timeOfDay) {
        RecurrenceFields.TIME_OF_DAY.valueOf(timeOfDay.hourOfDay,
                                             timeOfDay.minuteOfHour,
                                             timeOfDay.secondOfMinute)
    }

    private static ExplicitDate getDate(LocalDate date, LocalTime timeOfDay) {
        return new ExplicitDate(RecurrenceFields.YEAR.valueOf(date.year),
                                RecurrenceFields.MONTH_OF_YEAR.valueOf(date.monthOfYear),
                                RecurrenceFields.DAY_OF_MONTH.valueOf(date.dayOfMonth),
                                getTime(timeOfDay))
    }

    private static Collection<ExplicitDate> getDates(Set<LocalDate> dates, LocalTime timeOfDay) {
        dates.collect { date ->
            // examples from JDEV had the time in them
            getDate(date, timeOfDay)
        }
    }

    static Schedule getOracleSchedule(RecurringSchedule ourSchedule) {
        def frequency = frequencyMapping[ourSchedule.frequency]
        def recurrence = new Recurrence(frequency,
                                        ourSchedule.repeatInterval,
                                        ourSchedule.startDate.toDate().toCalendar(),
                                        ourSchedule.endDate.toDate().toCalendar())
        ourSchedule.daysOfWeek.each { day ->
            recurrence.addDayOfWeek(dayOfWeekMapping[day])
        }
        def timeOfDay = ourSchedule.timeOfDay
        recurrence.recurTime = getTime(timeOfDay)
        def oracle = new Schedule(ourSchedule.name,
                                  ourSchedule.description,
                                  recurrence)
        oracle.displayName = ourSchedule.displayName
        oracle.timeZone = ourSchedule.timeZone.toTimeZone()
        oracle.inclusionDates = getDates(ourSchedule.includeDates, timeOfDay)
        oracle.exclusionDates = getDates(ourSchedule.excludeDates, timeOfDay)
        oracle.validate()
        oracle
    }
}
