package com.avioconsulting.ess

import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar
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
                                         LocalTime timeOfDay,
                                         DateTimeZone timeZone,
                                         int repeatInterval,
                                         List<RecurringSchedule.DayOfWeek> daysOfWeek,
                                         Set<LocalDate> holidays) {
        // will call the other 2 methods in here
    }

    /**
     * When will the job run
     *
     * @param beginningDate - first date to start with
     * @param daysOfWeek - which days the job should run on
     * @param repeatInterval - Every x weeks
     * @return all dates the job runs on
     */
    static Set<LocalDate> getJobExecutionDates(LocalDate beginningDate,
                                               List<RecurringSchedule.DayOfWeek> daysOfWeek,
                                               int repeatInterval) {

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
