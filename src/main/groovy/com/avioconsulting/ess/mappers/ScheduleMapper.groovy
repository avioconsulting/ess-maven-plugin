package com.avioconsulting.ess.mappers

import com.avioconsulting.ess.models.RecurringSchedule
import oracle.as.scheduler.ExplicitDate
import oracle.as.scheduler.Recurrence
import oracle.as.scheduler.RecurrenceFields
import oracle.as.scheduler.Schedule
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
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

    private static RecurrenceFields.TIME_OF_DAY getLocalTime(LocalTime timeOfDay) {
        RecurrenceFields.TIME_OF_DAY.valueOf(timeOfDay.hourOfDay,
                                             timeOfDay.minuteOfHour,
                                             timeOfDay.secondOfMinute)
    }

    private static ExplicitDate getDateInServerTimeZone(LocalDate date,
                                                        LocalTime time,
                                                        DateTimeZone serverTimeZone) {
        // we can't specify a time zone and ESS will use the time zone of the server
        // when matching up these dates
        def ourDateTime = date.toDateTime(time)
        def serverDateTime = ourDateTime.toDateTime(serverTimeZone)
        new ExplicitDate(RecurrenceFields.YEAR.valueOf(serverDateTime.year),
                         RecurrenceFields.MONTH_OF_YEAR.valueOf(serverDateTime.monthOfYear),
                         RecurrenceFields.DAY_OF_MONTH.valueOf(serverDateTime.dayOfMonth),
                         getLocalTime(serverDateTime.toLocalTime()))
    }

    private static Collection<ExplicitDate> getExclInclDates(Set<LocalDate> dates,
                                                             LocalTime timeOfDay,
                                                             DateTimeZone serverTimeZone) {
        dates.collect { date ->
            // examples from JDEV had the time in them
            getDateInServerTimeZone(date, timeOfDay, serverTimeZone)
        }
    }

    static Schedule getOracleSchedule(RecurringSchedule ourSchedule,
                                      DateTimeZone serverTimeZone) {
        def frequency = frequencyMapping[ourSchedule.frequency]
        def recurrence = new Recurrence(frequency,
                                        ourSchedule.repeatInterval,
                                        ourSchedule.startDate.toDate().toCalendar(),
                                        ourSchedule.endDate.toDate().toCalendar())
        ourSchedule.daysOfWeek.each { day ->
            recurrence.addDayOfWeek(dayOfWeekMapping[day])
        }
        def timeOfDay = ourSchedule.timeOfDay
        recurrence.recurTime = getLocalTime(timeOfDay)
        def oracle = new Schedule(ourSchedule.name,
                                  ourSchedule.description,
                                  recurrence)
        oracle.displayName = ourSchedule.displayName
        oracle.timeZone = ourSchedule.timeZone.toTimeZone()
        oracle.inclusionDates = getExclInclDates(ourSchedule.includeDates, timeOfDay, serverTimeZone)
        oracle.exclusionDates = getExclInclDates(ourSchedule.excludeDates, timeOfDay, serverTimeZone)
        oracle.validate()
        oracle
    }
}
