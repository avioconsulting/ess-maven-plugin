package com.avioconsulting.ess.builders

import com.avioconsulting.ess.models.Direction
import com.avioconsulting.ess.models.RecurringSchedule
import com.avioconsulting.ess.models.RecurringSchedule.DayOfWeek
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar
import net.objectlab.kit.datecalc.common.HolidayCalendar
import net.objectlab.kit.datecalc.joda.LocalDateCalculator
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory
import org.joda.time.LocalDate

class ScheduleBuilder {
    static private final String NO_HOLIDAY_CALENDAR = 'NO_HOLIDAY_CALENDAR'

    static {
        HolidayCalendar<LocalDate> emptyHolidayCalendar = new DefaultHolidayCalendar<LocalDate>()
        LocalDateKitCalculatorsFactory.defaultInstance.registerHolidays(NO_HOLIDAY_CALENDAR,
                                                                        emptyHolidayCalendar)
    }

    /**
     * Builds a complete recurring schedule
     *
     * @param name
     * @param displayName
     * @param description
     * @param startDate
     * @param endDate
     * @param timeOfDay
     * @param timeZone
     * @param daysOfWeek
     * @param holidays
     * @param alternateDirection
     * @return
     */
    static RecurringSchedule getSchedule(map) {
        def daysOfWeek = map.daysOfWeek
        def jobDates = getJobExecutionDates(map.startDate,
                                            map.endDate,
                                            daysOfWeek)
        Set<LocalDate> holidays = map.holidays
        Set<LocalDate> excludeDates = holidays.intersect(jobDates)
        Set<LocalDate> includeDates = getAlternateDates(excludeDates,
                                                        map.alternateDirection,
                                                        holidays)
        return new RecurringSchedule(
                name: map.name,
                description: map.description,
                displayName: map.displayName,
                timeZone: map.timeZone,
                frequency: RecurringSchedule.Frequency.Weekly,
                startDate: map.startDate,
                endDate: map.endDate,
                repeatInterval: 1,
                daysOfWeek: daysOfWeek,
                timeOfDay: map.timeOfDay,
                includeDates: includeDates,
                excludeDates: excludeDates)
    }

    /**
     * When will the job run
     *
     * @param beginningDate - first date to start with
     * @param endDate - date to stop at
     * @param daysOfWeek - which days the job should run on
     * @return all dates the job runs on
     */
    private static Set<LocalDate> getJobExecutionDates(LocalDate beginningDate,
                                                       LocalDate endDate,
                                                       List<RecurringSchedule.DayOfWeek> daysOfWeek) {
        def calculator = LocalDateKitCalculatorsFactory.forwardCalculator(
                NO_HOLIDAY_CALENDAR)
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


    private static DayOfWeek getDayOfWeek(LocalDate date) {
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
    private static Set<LocalDate> getAlternateDates(Set<LocalDate> daysOnHolidays,
                                                    Direction direction,
                                                    Set<LocalDate> holidays) {
        def increment = direction == Direction.Forward ? 1 : -1
        def calculator = getCalculatorWithHolidays(direction,
                                                   holidays)
        daysOnHolidays.collect { date ->
            calculator.startDate = date
            // our start date is on a holiday, currentBusinessDate takes care of it
            if (date == calculator.currentBusinessDate) {
                calculator.moveByBusinessDays(increment)
            }
            calculator.currentBusinessDate
        }
    }

    private static LocalDateCalculator getCalculatorWithHolidays(Direction direction, Set<LocalDate> holidays) {
        HolidayCalendar<LocalDate> holidayCalendar = new DefaultHolidayCalendar<LocalDate>(holidays)
        // avoid global/static scope
        def id = UUID.randomUUID().toString()
        LocalDateKitCalculatorsFactory.defaultInstance.registerHolidays(id,
                                                                        holidayCalendar)
        return direction == Direction.Forward ?
                LocalDateKitCalculatorsFactory.forwardCalculator(id) :
                LocalDateKitCalculatorsFactory.backwardCalculator(id)
    }
}
