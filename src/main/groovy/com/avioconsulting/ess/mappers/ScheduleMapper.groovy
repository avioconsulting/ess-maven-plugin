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

import java.time.DayOfWeek

class ScheduleMapper {
    private final DateTimeZone thisMachineTimeZone
    private final DateTimeZone serverTimeZone

    ScheduleMapper(DateTimeZone serverTimeZone) {
        this.thisMachineTimeZone = DateTimeZone.default
        this.serverTimeZone = serverTimeZone
    }

    ScheduleMapper(DateTimeZone ourMachineZone, DateTimeZone serverTimeZone) {
        this.thisMachineTimeZone = ourMachineZone
        this.serverTimeZone = serverTimeZone
    }

    private static final Map<RecurringSchedule.Frequency, RecurrenceFields.FREQUENCY> frequencyMapping = [
            (RecurringSchedule.Frequency.Weekly): RecurrenceFields.FREQUENCY.WEEKLY,
            (RecurringSchedule.Frequency.Monthly): RecurrenceFields.FREQUENCY.MONTHLY
    ]

    private static final Map<DayOfWeek, RecurrenceFields.DAY_OF_WEEK> dayOfWeekMapping = [
            (DayOfWeek.MONDAY)   : RecurrenceFields.DAY_OF_WEEK.MONDAY,
            (DayOfWeek.TUESDAY)  : RecurrenceFields.DAY_OF_WEEK.TUESDAY,
            (DayOfWeek.WEDNESDAY): RecurrenceFields.DAY_OF_WEEK.WEDNESDAY,
            (DayOfWeek.THURSDAY) : RecurrenceFields.DAY_OF_WEEK.THURSDAY,
            (DayOfWeek.FRIDAY)   : RecurrenceFields.DAY_OF_WEEK.FRIDAY,
            (DayOfWeek.SATURDAY) : RecurrenceFields.DAY_OF_WEEK.SATURDAY,
            (DayOfWeek.SUNDAY)   : RecurrenceFields.DAY_OF_WEEK.SUNDAY
    ]

    private static RecurrenceFields.TIME_OF_DAY getTimeOfDay(LocalTime timeOfDay) {
        RecurrenceFields.TIME_OF_DAY.valueOf(timeOfDay.hourOfDay,
                                             timeOfDay.minuteOfHour,
                                             timeOfDay.secondOfMinute)
    }

    private static ExplicitDate getDateInDifferentTimeZone(LocalDate date,
                                                           DateTime timeOfDay,
                                                           DateTimeZone destinationTimeZone) {
        // we can't specify a time zone and ESS will use the time zone of the server
        // when matching up these dates
        def ourDateTime = date.toDateTime(timeOfDay.toLocalTime(), timeOfDay.zone)
        def serverDateTime = ourDateTime.toDateTime(destinationTimeZone)
        def localTime = serverDateTime.toLocalTime()
        new ExplicitDate(RecurrenceFields.YEAR.valueOf(serverDateTime.year),
                         RecurrenceFields.MONTH_OF_YEAR.valueOf(serverDateTime.monthOfYear),
                         RecurrenceFields.DAY_OF_MONTH.valueOf(serverDateTime.dayOfMonth),
                         getTimeOfDay(localTime))
    }

    private static Collection<ExplicitDate> getExclInclDates(Set<LocalDate> dates,
                                                             DateTime timeOfDay,
                                                             DateTimeZone destinationTimeZone) {
        dates.collect { date ->
            // examples from JDEV had the time in them
            getDateInDifferentTimeZone(date, timeOfDay, destinationTimeZone)
        }
    }

    Schedule getOracleSchedule(RecurringSchedule ourSchedule) {
        def frequency = frequencyMapping[ourSchedule.frequency]
        def recurrence = new Recurrence(frequency,
                                        ourSchedule.repeatInterval,
                                        ourSchedule.startDate.toDate().toCalendar(),
                                        ourSchedule.endDate.toDate().toCalendar())
        ourSchedule.daysOfWeek.each { day ->
            recurrence.addDayOfWeek(dayOfWeekMapping[day])
        }
        def dateTime = new LocalDate().toDateTime(ourSchedule.timeOfDay, ourSchedule.timeZone)
        def dateTimeThisMachine = dateTime.toDateTime(this.thisMachineTimeZone)
        // ESS seems to assume the recurTime is in the time zone of the machine running this (not necessarily server)
        recurrence.recurTime = getTimeOfDay(dateTimeThisMachine.toLocalTime())
        def oracle = new Schedule(ourSchedule.name,
                                  ourSchedule.description,
                                  recurrence)
        oracle.timeZone = ourSchedule.timeZone.toTimeZone()
        oracle.displayName = ourSchedule.displayName
        def exclInclDates = {
            Set<LocalDate> dates -> getExclInclDates(dates, dateTimeThisMachine, this.serverTimeZone)
        }
        oracle.inclusionDates = exclInclDates ourSchedule.includeDates
        oracle.exclusionDates = exclInclDates ourSchedule.excludeDates
        oracle.validate()
        oracle
    }
}
