package com.avioconsulting.ess

import org.joda.time.LocalDate
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class ScheduleBuilderTest {
    def getJobExecutionDates(LocalDate beginningDate,
                             LocalDate endDate,
                             List<RecurringSchedule.DayOfWeek> daysOfWeek) {
        Set<LocalDate> result = ScheduleBuilder.getJobExecutionDates(beginningDate,
                                                                     endDate,
                                                                     daysOfWeek)
        result.collect { date ->
            date.toString()
        }
    }

    def getAlternateDates(List<LocalDate> daysOnHolidays,
                          Direction direction,
                          List<LocalDate> holidays) {
        def result = ScheduleBuilder.getAlternateDates((Set<LocalDate>) daysOnHolidays,
                                                       direction,
                                                       (Set<LocalDate>) holidays)

        result.collect { date ->
            date.toString()
        }
    }

    @Test
    void getJobExecutionDates_BeginOnSunday_RepeatMondays_EndSameYear() {
        // arrange
        def beginningDate = new LocalDate(2017, 1, 1)

        // act
        def result = getJobExecutionDates(beginningDate,
                                          new LocalDate(2017, 2, 27),
                                          [RecurringSchedule.DayOfWeek.Monday])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-02',
                           '2017-01-09',
                           '2017-01-16',
                           '2017-01-23',
                           '2017-01-30',
                           '2017-02-06',
                           '2017-02-13',
                           '2017-02-20',
                           '2017-02-27'
                   ]))
    }

    @Test
    void getJobExecutionDates_BeginOnSunday_RepeatTuesdays_EndSameYear() {
        // arrange
        def beginningDate = new LocalDate(2017, 1, 1)

        // act
        def result = getJobExecutionDates(beginningDate,
                                          new LocalDate(2017, 2, 28),
                                          [RecurringSchedule.DayOfWeek.Tuesday])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-03',
                           '2017-01-10',
                           '2017-01-17',
                           '2017-01-24',
                           '2017-01-31',
                           '2017-02-07',
                           '2017-02-14',
                           '2017-02-21',
                           '2017-02-28'
                   ]))
    }

    @Test
    void getJobExecutionDates_BeginOnSunday_RepeatMondaysTuesdays_EndSameYear() {
        // arrange
        def beginningDate = new LocalDate(2017, 1, 1)

        // act
        def result = getJobExecutionDates(beginningDate,
                                          new LocalDate(2017, 2, 28),
                                          [
                                                  RecurringSchedule.DayOfWeek.Monday,
                                                  RecurringSchedule.DayOfWeek.Tuesday
                                          ])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-02',
                           '2017-01-03',
                           '2017-01-09',
                           '2017-01-10',
                           '2017-01-16',
                           '2017-01-17',
                           '2017-01-23',
                           '2017-01-24',
                           '2017-01-30',
                           '2017-01-31',
                           '2017-02-06',
                           '2017-02-07',
                           '2017-02-13',
                           '2017-02-14',
                           '2017-02-20',
                           '2017-02-21',
                           '2017-02-27',
                           '2017-02-28'
                   ]))
    }

    @Test
    void getJobExecutionDates_BeginOnSunday_RepeatMondaysTuesdays_SpecOrderBackwards_EndSameYear() {
        // arrange
        def beginningDate = new LocalDate(2017, 1, 1)

        // act
        def result = getJobExecutionDates(beginningDate,
                                          new LocalDate(2017, 2, 28),
                                          [
                                                  RecurringSchedule.DayOfWeek.Tuesday,
                                                  RecurringSchedule.DayOfWeek.Monday
                                          ])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-02',
                           '2017-01-03',
                           '2017-01-09',
                           '2017-01-10',
                           '2017-01-16',
                           '2017-01-17',
                           '2017-01-23',
                           '2017-01-24',
                           '2017-01-30',
                           '2017-01-31',
                           '2017-02-06',
                           '2017-02-07',
                           '2017-02-13',
                           '2017-02-14',
                           '2017-02-20',
                           '2017-02-21',
                           '2017-02-27',
                           '2017-02-28'
                   ]))
    }

    @Test
    void getAlternateDates_Backward() {
        // arrange
        def daysOnHolidays = [
                new LocalDate(2017, 1, 9),
                new LocalDate(2017, 1, 20)
        ]

        // act
        def result = getAlternateDates(daysOnHolidays, Direction.Backward, [])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-06',
                           '2017-01-19'
                   ]))
    }

    @Test
    void getAlternateDates_Forward() {
        // arrange
        def daysOnHolidays = [
                new LocalDate(2017, 1, 9),
                new LocalDate(2017, 1, 20)
        ]

        // act
        def result = getAlternateDates(daysOnHolidays, Direction.Forward, [])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-10',
                           '2017-01-23'
                   ]))
    }

    @Test
    void getAlternateDates_2DatesTogether() {
        // arrange
        def daysOnHolidays = [
                new LocalDate(2017, 1, 9),
                new LocalDate(2017, 1, 10)
        ]

        // act
        def result = getAlternateDates(daysOnHolidays,
                                       Direction.Backward,
                                       [new LocalDate(2017, 1, 9),
                                        new LocalDate(2017, 1, 10)])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-06' // only does 1 run the day before the 2 holidays
                   ]))
    }

    @Test
    void getAlternateDates_AnotherHoliday() {
        // arrange
        def daysOnHolidays = [
                new LocalDate(2017, 1, 9),
                new LocalDate(2017, 1, 20)
        ]

        // act
        def result = getAlternateDates(daysOnHolidays,
                                       Direction.Backward,
                                       [new LocalDate(2017, 1, 6)])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-05', // the 6th is a holiday
                           '2017-01-19'
                   ]))
    }
}
