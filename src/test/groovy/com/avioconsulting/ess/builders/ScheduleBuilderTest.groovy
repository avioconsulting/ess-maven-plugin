package com.avioconsulting.ess.builders

import com.avioconsulting.ess.models.Direction
import com.avioconsulting.ess.models.RecurringSchedule
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Test

import java.time.DayOfWeek

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class ScheduleBuilderTest {
    def getJobExecutionDates(LocalDate beginningDate,
                             LocalDate endDate,
                             List<DayOfWeek> daysOfWeek) {
        Set<LocalDate> result = ScheduleBuilder.getJobExecutionDates(beginningDate,
                                                                     endDate,
                                                                     daysOfWeek)
        result.collect { date ->
            date.toString()
        }
    }

    def getMonthlyJobExecutionDates(LocalDate beginningDate,
                                    LocalDate endDate,
                                    List<Integer> daysOfMonth) {
        Set<LocalDate> result = ScheduleBuilder.getMonthlyJobExecutionDates(beginningDate,
                                                                            endDate,
                                                                            daysOfMonth)
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
    void getWeeklySchedule() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getWeeklySchedule name: 'the_schedule',
                                                         displayName: 'the schedule',
                                                         description: 'Weekly schedule on mondays',
                                                         startDate: new LocalDate(2017, 1, 1),
                                                         endDate: new LocalDate(2017, 2, 27),
                                                         timeOfDay: new LocalTime(9, 15, 10),
                                                         timeZone: DateTimeZone.forID('America/Denver'),
                                                         daysOfWeek: [DayOfWeek.MONDAY],
                                                         holidays: [new LocalDate(2017, 1, 30)],
                                                         alternateDirection: Direction.Backward

        // assert
        // not using count to limit
        assertThat schedule.recurrenceCount,
                   is(equalTo(0))
        assertThat schedule.startDate,
                   is(equalTo(new LocalDate(2017, 1, 1)))
        assertThat schedule.endDate,
                   is(equalTo(new LocalDate(2017, 2, 27)))
        assertThat schedule.name,
                   is(equalTo('the_schedule'))
        assertThat schedule.displayName,
                   is(equalTo('the schedule'))
        assertThat schedule.description,
                   is(equalTo('Weekly schedule on mondays'))
        assertThat schedule.daysOfWeek,
                   is(equalTo([DayOfWeek.MONDAY]))
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
                   is(equalTo(new LocalDate(2017, 1, 27)))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(1))
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 30)))
    }

    @Test
    void getMonthlySchedule_WeekendsIncluded() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getMonthlySchedule name: 'the_schedule',
                                                          displayName: 'the schedule',
                                                          description: 'Monthly schedule',
                                                          startDate: new LocalDate(2017, 1, 1),
                                                          include: WeekendDates.Yes,
                                                          endDate: new LocalDate(2017, 2, 27),
                                                          timeOfDay: new LocalTime(9, 15, 10),
                                                          timeZone: DateTimeZone.forID('America/Denver'),
                                                          daysOfMonth: [1, 30],
                                                          holidays: [new LocalDate(2017, 1, 30)],
                                                          alternateDirection: Direction.Backward

        // assert
        // not using count to limit
        assertThat schedule.recurrenceCount,
                   is(equalTo(0))
        assertThat schedule.startDate,
                   is(equalTo(new LocalDate(2017, 1, 1)))
        assertThat schedule.endDate,
                   is(equalTo(new LocalDate(2017, 2, 27)))
        assertThat schedule.name,
                   is(equalTo('the_schedule'))
        assertThat schedule.displayName,
                   is(equalTo('the schedule'))
        assertThat schedule.description,
                   is(equalTo('Monthly schedule'))
        assertThat schedule.daysOfMonth,
                   is(equalTo([1, 30]))
        assertThat schedule.frequency,
                   is(equalTo(RecurringSchedule.Frequency.Monthly))
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
                   is(equalTo(new LocalDate(2017, 1, 27)))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(1))
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 30)))
    }

    @Test
    void getMonthlySchedule_WeekendsNotIncluded() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getMonthlySchedule name: 'the_schedule',
                                                          displayName: 'the schedule',
                                                          description: 'Monthly schedule',
                                                          startDate: new LocalDate(2017, 1, 1),
                                                          include: WeekendDates.No,
                                                          endDate: new LocalDate(2017, 2, 27),
                                                          timeOfDay: new LocalTime(9, 15, 10),
                                                          timeZone: DateTimeZone.forID('America/Denver'),
                                                          // 7th is a saturday, 1st is a sunday
                                                          daysOfMonth: [1, 7, 30],
                                                          holidays: [new LocalDate(2017, 1, 30)],
                                                          alternateDirection: Direction.Backward

        // assert
        // not using count to limit
        assertThat schedule.recurrenceCount,
                   is(equalTo(0))
        assertThat schedule.startDate,
                   is(equalTo(new LocalDate(2017, 1, 1)))
        assertThat schedule.endDate,
                   is(equalTo(new LocalDate(2017, 2, 27)))
        assertThat schedule.name,
                   is(equalTo('the_schedule'))
        assertThat schedule.displayName,
                   is(equalTo('the schedule'))
        assertThat schedule.description,
                   is(equalTo('Monthly schedule'))
        assertThat schedule.daysOfMonth,
                   is(equalTo([1, 7, 30]))
        assertThat schedule.frequency,
                   is(equalTo(RecurringSchedule.Frequency.Monthly))
        assertThat schedule.timeZone,
                   is(equalTo(DateTimeZone.forID('America/Denver')))
        // only supporting every 1 week right now
        assertThat schedule.repeatInterval,
                   is(equalTo(1))
        assertThat schedule.timeOfDay,
                   is(equalTo(new LocalTime(9, 15, 10)))
        assertThat schedule.includeDates.size(),
                   is(equalTo(2))
        // 6th is the substitute day for the 7th
        assertThat schedule.includeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 6)))
        // friday before the 30th since 30th is a holiday
        assertThat schedule.includeDates[1],
                   is(equalTo(new LocalDate(2017, 1, 27)))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(3))
        // 1st is a sunday
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 1)))
        // 7th is a saturday
        assertThat schedule.excludeDates[1],
                   is(equalTo(new LocalDate(2017, 1, 7)))
        assertThat schedule.excludeDates[2],
                   is(equalTo(new LocalDate(2017, 1, 30)))
    }

    @Test
    void getMonthlySchedule_WeekendsNotIncluded_AlternateIsHoliday() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getMonthlySchedule name: 'the_schedule',
                                                          displayName: 'the schedule',
                                                          description: 'Monthly schedule',
                                                          startDate: new LocalDate(2017, 1, 1),
                                                          include: WeekendDates.No,
                                                          endDate: new LocalDate(2017, 2, 27),
                                                          timeOfDay: new LocalTime(9, 15, 10),
                                                          timeZone: DateTimeZone.forID('America/Denver'),
                                                          // 7th is a saturday
                                                          daysOfMonth: [7],
                                                          holidays: [new LocalDate(2017, 1, 6)],
                                                          alternateDirection: Direction.Backward

        // assert
        assertThat schedule.includeDates.size(),
                   is(equalTo(1))
        // 6th is the alternate date for the weekend day but it's a holiday
        assertThat schedule.includeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 5)))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(1))
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 7)))
    }

    @Test
    void getWeeklySchedule_2ConsecutiveDays() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getWeeklySchedule name: 'the_schedule',
                                                         displayName: 'the schedule',
                                                         description: 'Weekly schedule',
                                                         startDate: new LocalDate(2017, 1, 1),
                                                         endDate: new LocalDate(2017, 2, 27),
                                                         timeOfDay: new LocalTime(9, 15, 10),
                                                         timeZone: DateTimeZone.forID('America/Denver'),
                                                         daysOfWeek: [DayOfWeek.MONDAY,
                                                                      DayOfWeek.TUESDAY],
                                                         holidays: [new LocalDate(2017, 1, 30),
                                                                    new LocalDate(2017, 1, 31)],
                                                         alternateDirection: Direction.Backward

        // assert
        assertThat schedule.recurrenceCount,
                   is(equalTo(0))
        assertThat schedule.includeDates.size(),
                   is(equalTo(1))
        // friday before the 30th since 30th is a holiday
        assertThat schedule.includeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 27)))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(2))
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 30)))
        assertThat schedule.excludeDates[1],
                   is(equalTo(new LocalDate(2017, 1, 31)))
    }

    @Test
    void getWeeklySchedule_InclusionDateIsRegularlyScheduledDate() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getWeeklySchedule name: 'the_schedule',
                                                         displayName: 'the schedule',
                                                         description: 'Weekly schedule',
                                                         startDate: new LocalDate(2017, 1, 1),
                                                         endDate: new LocalDate(2017, 2, 27),
                                                         timeOfDay: new LocalTime(9, 15, 10),
                                                         timeZone: DateTimeZone.forID('America/Denver'),
                                                         daysOfWeek: [DayOfWeek.MONDAY,
                                                                      DayOfWeek.TUESDAY],
                                                         holidays: [new LocalDate(2017, 1, 31)],
                                                         alternateDirection: Direction.Backward

        // assert
        assertThat schedule.recurrenceCount,
                   is(equalTo(0))
        // the alternate date for the 31st holiday is the 30th but that's a regular scheduled job date anyways
        assertThat schedule.includeDates.size(),
                   is(equalTo(0))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(1))
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 31)))
    }

    @Test
    void getWeeklySchedule_InclusionBeforeStart() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getWeeklySchedule name: 'the_schedule',
                                                         displayName: 'the schedule',
                                                         description: 'Weekly schedule',
                                                         startDate: new LocalDate(2017, 1, 1),
                                                         endDate: new LocalDate(2017, 2, 27),
                                                         timeOfDay: new LocalTime(9, 15, 10),
                                                         timeZone: DateTimeZone.forID('America/Denver'),
                                                         daysOfWeek: [DayOfWeek.MONDAY],
                                                         holidays: [new LocalDate(2017, 1, 2), new LocalDate(2017, 1,
                                                                                                             23)],
                                                         alternateDirection: Direction.Backward

        // assert
        assertThat schedule.recurrenceCount,
                   is(equalTo(0))
        assertThat schedule.includeDates.size(),
                   is(equalTo(1))
        // friday before the 23rd since 23rd is a holiday, the other would be inclusion date is 12/30/2016 which
        // is before the start date, so it's excluded
        assertThat schedule.includeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 20)))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(2))
        assertThat schedule.excludeDates[0],
                   is(equalTo(new LocalDate(2017, 1, 2)))
        assertThat schedule.excludeDates[1],
                   is(equalTo(new LocalDate(2017, 1, 23)))
    }

    @Test
    void getWeeklySchedule_Weekend() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getWeeklySchedule name: 'the_schedule',
                                                         displayName: 'the schedule',
                                                         description: 'Weekly schedule on mondays',
                                                         startDate: new LocalDate(2017, 1, 1),
                                                         endDate: new LocalDate(2017, 2, 27),
                                                         timeOfDay: new LocalTime(9, 15, 10),
                                                         timeZone: DateTimeZone.forID('America/Denver'),
                                                         daysOfWeek: [DayOfWeek.SUNDAY],
                                                         holidays: [],
                                                         alternateDirection: Direction.Backward

        // assert
        assertThat schedule.daysOfWeek,
                   is(equalTo([DayOfWeek.SUNDAY]))
        assertThat schedule.includeDates.size(),
                   is(equalTo(0))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(0))
    }

    @Test
    void getWeeklySchedule_NoHolidays() {
        // arrange

        // act
        def schedule = ScheduleBuilder.getWeeklySchedule name: 'the_schedule',
                                                         displayName: 'the schedule',
                                                         description: 'Weekly schedule on mondays',
                                                         startDate: new LocalDate(2017, 1, 1),
                                                         endDate: new LocalDate(2017, 2, 27),
                                                         timeOfDay: new LocalTime(9, 15, 10),
                                                         timeZone: DateTimeZone.forID('America/Denver'),
                                                         daysOfWeek: [DayOfWeek.MONDAY],
                                                         holidays: [],
                                                         alternateDirection: Direction.Backward

        // assert
        assertThat schedule.daysOfWeek,
                   is(equalTo([DayOfWeek.MONDAY]))
        assertThat schedule.includeDates.size(),
                   is(equalTo(0))
        assertThat schedule.excludeDates.size(),
                   is(equalTo(0))
    }

    @Test
    void getJobExecutionDates_BeginOnSunday_RepeatMondays_EndSameYear() {
        // arrange
        def beginningDate = new LocalDate(2017, 1, 1)

        // act
        def result = getJobExecutionDates(beginningDate,
                                          new LocalDate(2017, 2, 27),
                                          [DayOfWeek.MONDAY])

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
                                          [DayOfWeek.TUESDAY])

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
                                                  DayOfWeek.MONDAY,
                                                  DayOfWeek.TUESDAY
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
                                                  DayOfWeek.TUESDAY,
                                                  DayOfWeek.MONDAY
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
    void getJobExecutionDates_Weekend() {
        // arrange
        def beginningDate = new LocalDate(2017, 1, 1)

        // act
        def result = getJobExecutionDates(beginningDate,
                                          new LocalDate(2017, 1, 31),
                                          [DayOfWeek.SUNDAY])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-01',
                           '2017-01-08',
                           '2017-01-15',
                           '2017-01-22',
                           '2017-01-29'
                   ]))
    }

    @Test
    void getMonthlyJobExecutionDates() {
        // arrange
        def beginningDate = new LocalDate(2017, 1, 1)

        // act
        def result = getMonthlyJobExecutionDates(beginningDate,
                                                 new LocalDate(2017, 3, 31),
                                                 [1, 10])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-01',
                           '2017-01-10',
                           '2017-02-01',
                           '2017-02-10',
                           '2017-03-01',
                           '2017-03-10',
                   ]))
    }

    @Test
    void getMonthlyJobExecutionDates_ExceedsMonthEnd() {
        // arrange
        def beginningDate = new LocalDate(2017, 1, 1)

        // act
        def result = getMonthlyJobExecutionDates(beginningDate,
                                                 new LocalDate(2017, 3, 31),
                                                 [31])

        // assert
        assertThat result,
                   is(equalTo([
                           '2017-01-31',
                           '2017-03-31',
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
