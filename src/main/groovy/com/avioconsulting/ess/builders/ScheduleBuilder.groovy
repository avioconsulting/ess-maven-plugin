package com.avioconsulting.ess.builders

import com.avioconsulting.ess.models.Direction
import com.avioconsulting.ess.models.MonthlySchedule
import com.avioconsulting.ess.models.WeeklySchedule
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar
import net.objectlab.kit.datecalc.common.HolidayCalendar
import net.objectlab.kit.datecalc.joda.LocalDateCalculator
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory
import org.joda.time.LocalDate

import java.time.DayOfWeek

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
    static WeeklySchedule getWeeklySchedule(map) {
        List<DayOfWeek> daysOfWeek = map.daysOfWeek
        LocalDate startDate = map.startDate
        LocalDate endDate = map.endDate
        def jobDates = getJobExecutionDates(startDate,
                                            endDate,
                                            daysOfWeek)
        Set<LocalDate> holidays = map.holidays
        Direction alternateDirection = map.alternateDirection
        def (Set<LocalDate> includeDates, Collection<LocalDate> excludeDates) = getIncludeExcludeDates(holidays,
                                                                                                       alternateDirection,
                                                                                                       jobDates,
                                                                                                       startDate)
        new WeeklySchedule(name: map.name,
                           description: map.description,
                           displayName: map.displayName,
                           timeZone: map.timeZone,
                           startDate: startDate,
                           endDate: endDate,
                           repeatInterval: 1,
                           daysOfWeek: daysOfWeek,
                           timeOfDay: map.timeOfDay,
                           includeDates: includeDates,
                           excludeDates: excludeDates)
    }

    private static List getIncludeExcludeDates(Set<LocalDate> holidays,
                                               Direction alternateDirection,
                                               Set<LocalDate> jobDates,
                                               LocalDate startDate) {
        Set<LocalDate> excludeDates = holidays.intersect(jobDates)
        Set<LocalDate> includeDates = getAlternateDates(excludeDates,
                                                        alternateDirection,
                                                        holidays)
        includeDates = includeDates.findAll { LocalDate date ->
            // alternate dates before the start date don't make sense
            date >= startDate
        } - jobDates // no need to include already scheduled dates
        [includeDates, excludeDates]
    }

    static MonthlySchedule getMonthlySchedule(map) {
        List<Integer> daysOfMonth = map.daysOfMonth
        LocalDate startDate = map.startDate
        LocalDate endDate = map.endDate
        def jobDates = getMonthlyJobExecutionDates(startDate,
                                                   endDate,
                                                   daysOfMonth)
        Set<LocalDate> holidays = map.holidays
        Direction alternateDirection = map.alternateDirection
        def (Set<LocalDate> includeDates, Collection<LocalDate> excludeDates) = getIncludeExcludeDates(holidays,
                                                                                                       alternateDirection,
                                                                                                       jobDates,
                                                                                                       startDate)
        new MonthlySchedule(name: map.name,
                            description: map.description,
                            displayName: map.displayName,
                            timeZone: map.timeZone,
                            startDate: startDate,
                            endDate: endDate,
                            repeatInterval: 1,
                            daysOfMonth: daysOfMonth,
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
                                                       List<DayOfWeek> daysOfWeek) {
        Set<LocalDate> list = []
        def currentDate = new LocalDate(beginningDate)
        while (currentDate <= endDate) {
            if (daysOfWeek.contains(getDayOfWeek(currentDate))) {
                list << currentDate
            }
            currentDate = currentDate.plusDays(1)
        }
        list
    }

    private static Set<LocalDate> getMonthlyJobExecutionDates(LocalDate beginningDate,
                                                              LocalDate endDate,
                                                              List<Integer> daysOfMonth) {
        Set<LocalDate> list = []
        def currentDate = new LocalDate(beginningDate)
        while (currentDate <= endDate) {
            if (daysOfMonth.contains(currentDate.dayOfMonth)) {
                list << currentDate
            }
            currentDate = currentDate.plusDays(1)
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
