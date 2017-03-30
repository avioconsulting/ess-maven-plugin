package com.avioconsulting.ess

import com.avioconsulting.ess.factories.ScheduleFactory
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

@SuppressWarnings("GroovyAccessibility")
class ScheduleTest extends Common {
    @Test
    void NoExistingSchedule_CreatesWithoutError() {
        // arrange
        this.factories[ScheduleFactory] = [SingleScheduleFactory]
        def mojo = getJobScheduleMojo()

        // act
        mojo.execute()

        // assert
        def expectedSchedule = new SingleScheduleFactory().createSchedule()
        assertThat mojo.newSchedules,
                   is(equalTo([expectedSchedule]))
        assertThat mojo.updatedSchedules,
                   is(empty())
    }

    @Test
    void ExistingSchedule_Changed_UpdatesWithoutError() {
        // arrange
        this.factories[ScheduleFactory] = [SingleScheduleFactory]
        def mojo = getJobScheduleMojo()
        mojo.execute()
        this.factories[ScheduleFactory] = [UpdatedScheduleFactory]
        mojo = getJobScheduleMojo()

        // act
        mojo.execute()

        // assert
        def expectedSchedule = new UpdatedScheduleFactory().createSchedule()
        assertThat mojo.updatedSchedules,
                   is(equalTo([expectedSchedule]))
        assertThat mojo.newSchedules,
                   is(empty())
    }
}
