package com.avioconsulting.ess

import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.factories.JobRequestFactory
import com.avioconsulting.ess.factories.ScheduleFactory
import com.avioconsulting.ess.models.JobRequest
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

class JobRequestTest extends Common {
    class DummyFactory implements JobRequestFactory {
        static JobRequest returnThis

        JobRequest createJobRequest() {
            returnThis
        }
    }

    @Test
    void NoExistingRequest_CreatesWithoutError() {
        // arrange
        this.factories[ScheduleFactory] = [SingleScheduleFactory]
        this.factories[JobDefinitionFactory] = [SingleJobDefFactory]
        def expectedJobDef = new SingleJobDefFactory().createJobDefinition()
        def expectedSchedule = new SingleScheduleFactory().createSchedule()
        DummyFactory.returnThis = new JobRequest(submissionNotes: 'the notes',
                                                 jobDefinition: expectedJobDef,
                                                 schedule: expectedSchedule)
        this.factories[JobRequestFactory] = [DummyFactory]
        def mojo = getJobScheduleMojo()

        // act
        mojo.execute()

        // assert
        def expectedJobRequest = DummyFactory.returnThis
        assertThat mojo.newJobDefs,
                   is(equalTo([expectedJobDef]))
        assertThat mojo.updateJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(empty())
        assertThat mojo.newSchedules,
                   is(equalTo([expectedSchedule]))
        assertThat mojo.updatedSchedules,
                   is(empty())
        assertThat mojo.newJobRequests,
                   is(equalTo([expectedJobRequest]))
        assertThat mojo.updatedJobRequests,
                   is(empty())
    }

    @Test
    void ExistingRequest_NothingChanged() {
        // arrange

        // act

        // assert
        fail 'write this'
    }

    @Test
    void ExistingRequest_JobParametersChange_UpdatesWithoutError() {
        // arrange
        this.factories[ScheduleFactory] = [SingleScheduleFactory]
        this.factories[JobDefinitionFactory] = [SingleJobDefFactory]
        def expectedSchedule = new SingleScheduleFactory().createSchedule()
        DummyFactory.returnThis = new JobRequest(submissionNotes: 'the notes',
                                                 jobDefinition: new SingleJobDefFactory().createJobDefinition(),
                                                 schedule: expectedSchedule)
        this.factories[JobRequestFactory] = [DummyFactory]
        def mojo = getJobScheduleMojo()
        mojo.execute()
        this.factories[JobDefinitionFactory] = [UpdatedJobDefFactory]
        def expectedJobDef = new UpdatedJobDefFactory().createJobDefinition()
        DummyFactory.returnThis = new JobRequest(submissionNotes: 'the notes',
                                                 jobDefinition: expectedJobDef,
                                                 schedule: expectedSchedule)
        mojo = getJobScheduleMojo()

        // act
        println 'issuing 2nd execute call for update'
        mojo.execute()

        // assert
        def expectedJobRequest = DummyFactory.returnThis
        assertThat mojo.newJobDefs,
                   is(empty())
        assertThat mojo.updateJobDefs,
                   is(equalTo([expectedJobDef]))
        assertThat mojo.canceledJobDefs,
                   is(empty())
        assertThat mojo.newSchedules,
                   is(empty())
        assertThat mojo.updatedSchedules,
                   is(empty())
        assertThat mojo.newJobRequests,
                   is(empty())
        assertThat mojo.updatedJobRequests,
                   is(equalTo([expectedJobRequest]))
    }

    @Test
    void ExistingRequest_JobTypeChanges_RecreatesWithoutError() {
        // arrange

        // act

        // assert
        fail 'write this'
    }

    @Test
    void ExistingRequest_ScheduleChanges_UpdatesWithoutError() {
        // arrange

        // act

        // assert
        fail 'write this'
    }
}
