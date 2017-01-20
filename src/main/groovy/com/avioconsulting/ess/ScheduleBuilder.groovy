package com.avioconsulting.ess

import com.avioconsulting.ess.RecurringSchedule.DayOfWeek
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar
import net.objectlab.kit.datecalc.common.HolidayCalendar
import net.objectlab.kit.datecalc.joda.LocalDateCalculator
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

class ScheduleBuilder {
    static private final String NO_HOLIDAY_CALENDAR = 'NO_HOLIDAY_CALENDAR'

    static {
        HolidayCalendar<LocalDate> emptyHolidayCalendar = new DefaultHolidayCalendar<LocalDate>()
        LocalDateKitCalculatorsFactory.defaultInstance.registerHolidays(NO_HOLIDAY_CALENDAR, emptyHolidayCalendar)
    }

    static RecurringSchedule getSchedule(String name,
                                         String displayName,
                                         String description,
                                         LocalDate beginningDate,
                                         LocalDate endDate,
                                         LocalTime timeOfDay,
                                         DateTimeZone timeZone,
                                         List<RecurringSchedule.DayOfWeek> daysOfWeek,
                                         Set<LocalDate> holidays) {
        // will call the other 2 methods in here
    }

    /**
     * When will the job run
     *
     * @param beginningDate - first date to start with
     * @param endDate - date to stop at
     * @param daysOfWeek - which days the job should run on
     * @return all dates the job runs on
     */
    static Set<LocalDate> getJobExecutionDates(LocalDate beginningDate,
                                               LocalDate endDate,
                                               List<RecurringSchedule.DayOfWeek> daysOfWeek) {
        def calculator = LocalDateKitCalculatorsFactory.forwardCalculator(NO_HOLIDAY_CALENDAR)
        calculator.startDate = beginningDate
        Set<LocalDate> list = []
        // don't want order they're listed to matter
        daysOfWeek = daysOfWeek.sort()
        while (calculator.currentBusinessDate <= endDate) {
            daysOfWeek.each { day ->
                while (getDayOfWeek(calculator.currentBusinessDate) != day) {
                    calculator.moveByBusinessDays(1)
                }
                list << calculator.currentBusinessDate
            }
            calculator.moveByBusinessDays(1)
        }
        list
    }


    static DayOfWeek getDayOfWeek(LocalDate date) {
        int day = date.dayOfWeek
        // 1 based
        DayOfWeek.values()[day - 1]
    }

    /**
     * Calculates alternate job dates for holidays
     *
     * @param daysOnHolidays - job dates that fall on holidays
     * @param direction - direction to search for the nearest business day
     * @return - exclusion dates for ESS
     */
    static Set<LocalDate> getAlternateDates(Set<LocalDate> daysOnHolidays,
                                            Direction direction,
                                            Set<LocalDate> holidays) {
        def increment = direction == Direction.Forward ? 1 : -1
        def calculator = getCalculatorWithHolidays(direction, holidays)
        daysOnHolidays.collect { date ->
            calculator.startDate = date
            // our start date is on a holiday, currentBusinessDate takes care of it
            if (date == calculator.currentBusinessDate) {
                calculator.moveByBusinessDays(increment)
            }
            calculator.currentBusinessDate
        }
    }

    static LocalDateCalculator getCalculatorWithHolidays(Direction direction, Set<LocalDate> holidays) {
        HolidayCalendar<LocalDate> holidayCalendar = new DefaultHolidayCalendar<LocalDate>(holidays)
        // avoid global/static scope
        def id = UUID.randomUUID().toString()
        LocalDateKitCalculatorsFactory.defaultInstance.registerHolidays(id, holidayCalendar)
        return direction == Direction.Forward ?
                LocalDateKitCalculatorsFactory.forwardCalculator(id) :
                LocalDateKitCalculatorsFactory.backwardCalculator(id)
    }
}
