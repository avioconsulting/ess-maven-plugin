package com.avioconsulting.ess.mappers

import com.avioconsulting.ess.models.RecurringSchedule
import oracle.as.scheduler.ExplicitDate
import oracle.as.scheduler.RecurrenceFields
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Test

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
        def enumValues = RecurringSchedule.DayOfWeek.values()

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
    void getOracleSchedule() {
        // arrange
        def schedule = new RecurringSchedule(
                name: 'the_sch_name',
                description: 'the description',
                displayName: 'the display name',
                timeZone: DateTimeZone.forID('America/Denver'),
                frequency: RecurringSchedule.Frequency.Weekly,
                startDate: new LocalDate(2017, 1, 1),
                endDate: new LocalDate(2017, 2, 1),
                repeatInterval: 1,
                daysOfWeek: [RecurringSchedule.DayOfWeek.Monday,
                             RecurringSchedule.DayOfWeek.Tuesday],
                timeOfDay: new LocalTime(9, 15, 10),
                includeDates: [new LocalDate(2017, 1, 15), new LocalDate(2017, 1, 16)],
                excludeDates: [new LocalDate(2017, 1, 17), new LocalDate(2017, 1, 18)])

        // act
        def result = ScheduleMapper.getOracleSchedule(schedule, DateTimeZone.forID('America/Chicago'))

        // assert
        assertThat result.name,
                   is(equalTo('the_sch_name'))
        assertThat result.description,
                   is(equalTo('the description'))
        assertThat result.displayName,
                   is(equalTo('the display name'))
        assertThat result.timeZone,
                   is(equalTo(schedule.timeZone.toTimeZone()))
        assertThat fromExplicitDates(result.inclusionDates),
                   is(equalTo(['2017-01-15 10:15:10', '2017-01-16 10:15:10']))
        assertThat fromExplicitDates(result.exclusionDates),
                   is(equalTo(['2017-01-17 10:15:10', '2017-01-18 10:15:10']))
        def recurrence = result.recurrence
        assertThat recurrence.frequency,
                   is(equalTo(RecurrenceFields.FREQUENCY.WEEKLY))
        assertThat recurrence.startDate,
                   is(equalTo(schedule.startDate.toDate().toCalendar()))
        assertThat recurrence.endDate,
                   is(equalTo(schedule.endDate.toDate().toCalendar()))
        assertThat recurrence.interval,
                   is(equalTo(1))
        assertThat recurrence.daysOfWeek,
                   is(equalTo([RecurrenceFields.DAY_OF_WEEK.MONDAY, RecurrenceFields.DAY_OF_WEEK.TUESDAY]))
        assertThat recurrence.recurTime.toString(),
                   is(equalTo('9:15:10'))
    }
}
