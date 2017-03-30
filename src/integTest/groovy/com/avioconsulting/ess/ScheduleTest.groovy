package com.avioconsulting.ess

import com.avioconsulting.ess.factories.ScheduleFactory
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

class ScheduleTest extends Common {
    @Test
    void NoExistingSchedule_CreatesWithoutError() {
        // arrange
        this.factories[ScheduleFactory] = [SingleScheduleFactory]
        def mojo = getJobScheduleMojo()

        // act
        mojo.execute()

        // assert
        fail 'write this'
    }
}
