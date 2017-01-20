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
    enum Direction {
        Forward,
        Backward
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
        HolidayCalendar<LocalDate> holidayCalendar = new DefaultHolidayCalendar<LocalDate>()
        LocalDateKitCalculatorsFactory.defaultInstance.registerHolidays('NO_HOLIDAYS', holidayCalendar)
        def calculator = LocalDateKitCalculatorsFactory.forwardCalculator('NO_HOLIDAYS')
        calculator.startDate = beginningDate
        def list = []
        while (calculator.currentBusinessDate <= endDate) {
            while (getDayOfWeek(calculator.currentBusinessDate) != daysOfWeek[0]) {
                calculator.moveByBusinessDays(1)
            }
            list << calculator.currentBusinessDate
            calculator.moveByBusinessDays(5)
        }
        list
    }

    static DayOfWeek getDayOfWeek(LocalDate date) {
        int day = date.dayOfWeek
        // 1 based
        DayOfWeek.values()[day - 1]
    }

    /**
     * TODO: See if we need this, it might be simple subtraction
     * @param jobDates
     * @param holidays
     * @return - exclusion dates for ESS
     */
    static Set<LocalDate> getDaysOnHolidays(Set<LocalDate> jobDates,
                                            Set<LocalDate> holidays) {
    }

    /**
     * Calculates alternate job dates for holidays
     *
     * @param daysOnHolidays - job dates that fall on holidays
     * @param direction - direction to search for the nearest business day
     * @return - exclusion dates for ESS
     */
    static Set<LocalDate> getAlternateDates(Set<LocalDate> daysOnHolidays,
                                            Direction direction) {

    }


    def foo() {
        def holidays = new DefaultHolidayCalendar<LocalDate>()
        LocalDateKitCalculatorsFactory.registerHolidays('EVHC_2017')
        LocalDateCalculator calc = LocalDateKitCalculatorsFactory.forwardCalculator("EVHC_2017")
    }
}
