package com.avioconsulting.ess.builders

import com.avioconsulting.ess.builders.ScheduleBuilder
import com.avioconsulting.ess.models.Direction
import com.avioconsulting.ess.models.RecurringSchedule
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
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
    void getSchedule() {
        // arrange
        def builder = new ScheduleBuilder(new LocalDate(2017, 1, 1))

        // act
        def schedule = builder.getSchedule name: 'the_schedule',
                                           displayName: 'the schedule',
                                           description: 'Weekly schedule on mondays',
                                           endDate: new LocalDate(2017, 2, 27),
                                           timeOfDay: new LocalTime(9, 15, 10),
                                           timeZone: DateTimeZone.forID('America/Denver'),
                                           daysOfWeek: [RecurringSchedule.DayOfWeek.Monday],
                                           holidays: [new LocalDate(2017, 1, 30)],
                                           alternateDirection: Direction.Backward

        // assert
        // should have 9 weeks of execution
        assertThat schedule.recurrenceCount,
                   is(equalTo(9))
        assertThat schedule.name,
                   is(equalTo('the_schedule'))
        assertThat schedule.displayName,
                   is(equalTo('the schedule'))
        assertThat schedule.description,
                   is(equalTo('Weekly schedule on mondays'))
        assertThat schedule.daysOfWeek,
                   is(equalTo([RecurringSchedule.DayOfWeek.Monday]))
        assertThat schedule.frequency,
                   is(equalTo(RecurringSchedule.Frequency.Weekly))
        assertThat schedule.timeZone,
                   is(equalTo(DateTimeZone.forID('America/Denver')))
        // only supporting every 1 week right now
        assertThat schedule.repeatInterval,
                   is(equalTo(1))
        assertThat schedule.timeOfDay,
                   is(equalTo(new LocalTime(9, 15, 10)))
        assertThat schedule.includeDates.size(),
                   is(equalTo(1))
        // friday before the 30th since 30th is a holiday
        assertThat schedule.includeDates[0],
                   is(equalTo(new LocalDateTime(2017, 1, 27, 9, 15, 10)))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(1))
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDateTime(2017, 1, 30, 9, 15, 10)))
    }

    @Test
    void getSchedule_2ConsecutiveDays() {
        // arrange
        def builder = new ScheduleBuilder(new LocalDate(2017, 1, 1))

        // act
        def schedule = builder.getSchedule name: 'the_schedule',
                                           displayName: 'the schedule',
                                           description: 'Weekly schedule',
                                           endDate: new LocalDate(2017, 2, 27),
                                           timeOfDay: new LocalTime(9, 15, 10),
                                           timeZone: DateTimeZone.forID('America/Denver'),
                                           daysOfWeek: [RecurringSchedule.DayOfWeek.Monday,
                                                        RecurringSchedule.DayOfWeek.Tuesday],
                                           holidays: [new LocalDate(2017, 1, 30),
                                                      new LocalDate(2017, 1, 31)],
                                           alternateDirection: Direction.Backward

        // assert
        // would normally be 18 executions but the holidays for monday/tuesday intersect
        assertThat schedule.recurrenceCount,
                   is(equalTo(17))
        assertThat schedule.includeDates.size(),
                   is(equalTo(1))
        // friday before the 30th since 30th is a holiday
        assertThat schedule.includeDates[0],
                   is(equalTo(new LocalDateTime(2017, 1, 27, 9, 15, 10)))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(2))
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDateTime(2017, 1, 30, 9, 15, 10)))
        assertThat schedule.excludeDates[1],
                   is(equalTo(new LocalDateTime(2017, 1, 31, 9, 15, 10)))
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
