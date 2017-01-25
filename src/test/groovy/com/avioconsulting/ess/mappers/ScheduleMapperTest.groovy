package com.avioconsulting.ess.mappers

import com.avioconsulting.ess.models.MonthlySchedule
import com.avioconsulting.ess.models.RecurringSchedule
import com.avioconsulting.ess.models.WeeklySchedule
import oracle.as.scheduler.ExplicitDate
import oracle.as.scheduler.RecurrenceFields
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Test

import java.time.DayOfWeek

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

class ScheduleMapperTest {
    @Test
    void frequenciesMapped() {
        // arrange
        def enumValues = RecurringSchedule.Frequency.values()

        enumValues.each { type ->
            // act
            def result = ScheduleMapper.frequencyMapping[type]

            // assert
            assertThat "ENUM value ${type}",
                       result,
                       is(not(nullValue()))
        }
    }

    @Test
    void dayOfWeekMapped() {
        // arrange
        def enumValues = DayOfWeek.values()

        enumValues.each { type ->
            // act
            def result = ScheduleMapper.dayOfWeekMapping[type]

            // assert
            assertThat "ENUM value ${type}",
                       result,
                       is(not(nullValue()))
        }
    }

    List<String> fromExplicitDates(List<ExplicitDate> oracleDates) {
        oracleDates.collect { date ->
            new DateTime(date.toCalendar().getTime())
                    .toLocalDateTime()
                    .toString('YYYY-MM-DD HH:mm:ss')
        }
    }

    @Test
    void getOracleSchedule_Description() {
        // arrange
        def schedule = new WeeklySchedule(
                name: 'the_sch_name',
                description: 'the description',
                displayName: 'the display name',
                timeZone: DateTimeZone.forID('America/New_York'),
                startDate: new LocalDate(2017, 1, 1),
                endDate: new LocalDate(2017, 2, 1),
                repeatInterval: 1,
                daysOfWeek: [DayOfWeek.MONDAY,
                             DayOfWeek.TUESDAY],
                timeOfDay: new LocalTime(9, 15, 10),
                includeDates: [new LocalDate(2017, 1, 15), new LocalDate(2017, 1, 16)],
                excludeDates: [new LocalDate(2017, 1, 17), new LocalDate(2017, 1, 18)])

        // act
        def result = new ScheduleMapper(DateTimeZone.forID('America/Los_Angeles'),
                                        DateTimeZone.forID('America/Chicago')).getOracleSchedule(schedule)

        // assert
        assertThat result.name,
                   is(equalTo('the_sch_name'))
        assertThat result.description,
                   is(equalTo('the description'))
        assertThat result.displayName,
                   is(equalTo('the display name'))
        def recurrence = result.recurrence
        assertThat recurrence.interval,
                   is(equalTo(1))
        assertThat recurrence.daysOfWeek,
                   is(equalTo([RecurrenceFields.DAY_OF_WEEK.MONDAY, RecurrenceFields.DAY_OF_WEEK.TUESDAY]))
        assertThat recurrence.frequency,
                   is(equalTo(RecurrenceFields.FREQUENCY.WEEKLY))
    }

    @Test
    void getOracleSchedule_serverCentral_machineLA_jobNY() {
        // arrange
        def schedule = new WeeklySchedule(
                name: 'the_sch_name',
                description: 'the description',
                displayName: 'the display name',
                timeZone: DateTimeZone.forID('America/New_York'),
                startDate: new LocalDate(2017, 1, 1),
                endDate: new LocalDate(2017, 2, 1),
                repeatInterval: 1,
                daysOfWeek: [DayOfWeek.MONDAY,
                             DayOfWeek.TUESDAY],
                timeOfDay: new LocalTime(9, 15, 10),
                includeDates: [new LocalDate(2017, 1, 15), new LocalDate(2017, 1, 16)],
                excludeDates: [new LocalDate(2017, 1, 17), new LocalDate(2017, 1, 18)])

        // act
        def result = new ScheduleMapper(DateTimeZone.forID('America/Los_Angeles'),
                                        DateTimeZone.forID('America/Chicago')).getOracleSchedule(schedule)

        // assert
        assertThat result.timeZone,
                   is(equalTo(schedule.timeZone.toTimeZone()))
        assertThat fromExplicitDates(result.inclusionDates),
                   is(equalTo(['2017-01-15 08:15:10', '2017-01-16 08:15:10']))
        assertThat fromExplicitDates(result.exclusionDates),
                   is(equalTo(['2017-01-17 08:15:10', '2017-01-18 08:15:10']))
        def recurrence = result.recurrence
        assertThat recurrence.startDate,
                   is(equalTo(schedule.startDate.toDate().toCalendar()))
        assertThat recurrence.endDate,
                   is(equalTo(schedule.endDate.toDate().toCalendar()))
        assertThat recurrence.recurTime.toString(),
                   is(equalTo('6:15:10'))
    }

    @Test
    void getOracleSchedule_serverCentral_machineLA_jobCentral() {
        // arrange
        def schedule = new WeeklySchedule(
                name: 'the_sch_name',
                description: 'the description',
                displayName: 'the display name',
                timeZone: DateTimeZone.forID('America/Chicago'),
                startDate: new LocalDate(2017, 1, 1),
                endDate: new LocalDate(2017, 2, 1),
                repeatInterval: 1,
                daysOfWeek: [DayOfWeek.MONDAY,
                             DayOfWeek.TUESDAY],
                timeOfDay: new LocalTime(9, 15, 10),
                includeDates: [new LocalDate(2017, 1, 15), new LocalDate(2017, 1, 16)],
                excludeDates: [new LocalDate(2017, 1, 17), new LocalDate(2017, 1, 18)])

        // act
        def result = new ScheduleMapper(DateTimeZone.forID('America/Los_Angeles'),
                                        DateTimeZone.forID('America/Chicago')).getOracleSchedule(schedule)

        // assert
        assertThat result.timeZone,
                   is(equalTo(schedule.timeZone.toTimeZone()))
        assertThat fromExplicitDates(result.inclusionDates),
                   is(equalTo(['2017-01-15 09:15:10', '2017-01-16 09:15:10']))
        assertThat fromExplicitDates(result.exclusionDates),
                   is(equalTo(['2017-01-17 09:15:10', '2017-01-18 09:15:10']))
        def recurrence = result.recurrence
        assertThat recurrence.startDate,
                   is(equalTo(schedule.startDate.toDate().toCalendar()))
        assertThat recurrence.endDate,
                   is(equalTo(schedule.endDate.toDate().toCalendar()))
        assertThat recurrence.recurTime.toString(),
                   is(equalTo('7:15:10'))
    }

    @Test
    void getOracleSchedule_serverCentral_machineCentral_jobCentral() {
        // arrange
        def schedule = new WeeklySchedule(
                name: 'the_sch_name',
                description: 'the description',
                displayName: 'the display name',
                timeZone: DateTimeZone.forID('America/Chicago'),
                startDate: new LocalDate(2017, 1, 1),
                endDate: new LocalDate(2017, 2, 1),
                repeatInterval: 1,
                daysOfWeek: [DayOfWeek.MONDAY,
                             DayOfWeek.TUESDAY],
                timeOfDay: new LocalTime(9, 15, 10),
                includeDates: [new LocalDate(2017, 1, 15), new LocalDate(2017, 1, 16)],
                excludeDates: [new LocalDate(2017, 1, 17), new LocalDate(2017, 1, 18)])

        // act
        def result = new ScheduleMapper(DateTimeZone.forID('America/Chicago'),
                                        DateTimeZone.forID('America/Chicago')).getOracleSchedule(schedule)

        // assert
        assertThat result.timeZone,
                   is(equalTo(schedule.timeZone.toTimeZone()))
        assertThat fromExplicitDates(result.inclusionDates),
                   is(equalTo(['2017-01-15 09:15:10', '2017-01-16 09:15:10']))
        assertThat fromExplicitDates(result.exclusionDates),
                   is(equalTo(['2017-01-17 09:15:10', '2017-01-18 09:15:10']))
        def recurrence = result.recurrence
        assertThat recurrence.startDate,
                   is(equalTo(schedule.startDate.toDate().toCalendar()))
        assertThat recurrence.endDate,
                   is(equalTo(schedule.endDate.toDate().toCalendar()))
        assertThat recurrence.recurTime.toString(),
                   is(equalTo('9:15:10'))
    }

    @Test
    void getOracleSchedule_Monthly() {
        // arrange
        def schedule = new MonthlySchedule(
                name: 'the_sch_name',
                description: 'the description',
                displayName: 'the display name',
                timeZone: DateTimeZone.forID('America/Chicago'),
                startDate: new LocalDate(2017, 1, 1),
                endDate: new LocalDate(2017, 2, 1),
                repeatInterval: 1,
                daysOfMonth: [1, 10],
                timeOfDay: new LocalTime(9, 15, 10),
                includeDates: [new LocalDate(2017, 1, 15), new LocalDate(2017, 1, 16)],
                excludeDates: [new LocalDate(2017, 1, 17), new LocalDate(2017, 1, 18)])

        // act
        def result = new ScheduleMapper(DateTimeZone.forID('America/Chicago'),
                                        DateTimeZone.forID('America/Chicago')).getOracleSchedule(schedule)

        // assert
        assertThat result.name,
                   is(equalTo('the_sch_name'))
        assertThat result.description,
                   is(equalTo('the description'))
        assertThat result.displayName,
                   is(equalTo('the display name'))
        def recurrence = result.recurrence
        assertThat recurrence.interval,
                   is(equalTo(1))
        assertThat recurrence.daysOfMonth.collect { day -> day.value() },
                   is(equalTo([1, 10]))
        assertThat recurrence.frequency,
                   is(equalTo(RecurrenceFields.FREQUENCY.MONTHLY))
        assertThat result.timeZone,
                   is(equalTo(schedule.timeZone.toTimeZone()))
        assertThat fromExplicitDates(result.inclusionDates),
                   is(equalTo(['2017-01-15 09:15:10', '2017-01-16 09:15:10']))
        assertThat fromExplicitDates(result.exclusionDates),
                   is(equalTo(['2017-01-17 09:15:10', '2017-01-18 09:15:10']))
        assertThat recurrence.startDate,
                   is(equalTo(schedule.startDate.toDate().toCalendar()))
        assertThat recurrence.endDate,
                   is(equalTo(schedule.endDate.toDate().toCalendar()))
        assertThat recurrence.recurTime.toString(),
                   is(equalTo('9:15:10'))
    }
}
