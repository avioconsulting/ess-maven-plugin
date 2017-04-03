package com.avioconsulting.ess

import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.factories.JobRequestFactory
import com.avioconsulting.ess.factories.ScheduleFactory
import com.avioconsulting.ess.mappers.JobDefMapper
import com.avioconsulting.ess.models.JobDefinition
import com.avioconsulting.ess.models.JobRequest
import com.avioconsulting.ess.models.RecurringSchedule
import com.avioconsulting.ess.mojos.JobScheduleMojo
import com.avioconsulting.ess.wrappers.MetadataServiceWrapper
import com.avioconsulting.ess.wrappers.RuntimeServiceWrapper
import oracle.as.scheduler.Filter
import oracle.as.scheduler.RequestDetail
import oracle.as.scheduler.RuntimeService
import oracle.as.scheduler.State
import org.joda.time.LocalDateTime
import org.junit.Test
import sun.misc.Request

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

@SuppressWarnings("GroovyAccessibility")
class JobRequestTest extends Common {
    enum JobRequestType {
        Parent,
        Child
    }

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
        def childDetails = getJobRequestDetails(mojo,
                                                expectedSchedule,
                                                expectedJobDef,
                                                JobRequestType.Child
        )
        assertThat childDetails.size(),
                   is(equalTo(1))
        def detail = childDetails[0]
        assertThat detail.parameters.getValue(JobDefMapper.WSDL_OPERATION) as String,
                   is(equalTo(expectedJobDef.operation))
    }

    @Test
    void ExistingRequest_NothingChanged() {
        // arrange

        // act

        // assert
        // TODO: Verify that the request ID doesn't change at all
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
        def expectedUpdatedJobDef = new UpdatedJobDefFactory().createJobDefinition()
        DummyFactory.returnThis = new JobRequest(submissionNotes: 'the notes',
                                                 jobDefinition: expectedUpdatedJobDef,
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
                   is(equalTo([expectedUpdatedJobDef]))
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
        def childDetails = getJobRequestDetails(mojo,
                                                expectedSchedule,
                                                expectedUpdatedJobDef,
                                                JobRequestType.Child)
        assertThat childDetails.size(),
                   is(equalTo(1))
        def detail = childDetails[0]
        assertThat detail.parameters.getValue(JobDefMapper.WSDL_OPERATION) as String,
                   is(equalTo(expectedUpdatedJobDef.operation))
        def parentDetails = getJobRequestDetails(mojo,
                                                 expectedSchedule,
                                                 expectedUpdatedJobDef,
                                                 JobRequestType.Parent
        )
        assertThat parentDetails.size(),
                   is(equalTo(1))
        detail = parentDetails[0]
        assertThat detail.parameters.getValue(JobDefMapper.WSDL_OPERATION) as String,
                   is(equalTo(expectedUpdatedJobDef.operation))
    }

    @Test
    void ExistingRequest_JobTypeChanges_RecreatesWithoutError() {
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
        this.factories[JobDefinitionFactory] = [UpdatedJobTypeJobDefFactory]
        def expectedUpdatedJobDef = new UpdatedJobTypeJobDefFactory().createJobDefinition()
        DummyFactory.returnThis = new JobRequest(submissionNotes: 'the notes',
                                                 jobDefinition: expectedUpdatedJobDef,
                                                 schedule: expectedSchedule)
        mojo = getJobScheduleMojo()

        // act
        println 'issuing 2nd execute call for update'
        mojo.execute()

        // assert
        def expectedJobRequest = DummyFactory.returnThis
        assertThat mojo.newJobDefs,
                   is(equalTo([expectedUpdatedJobDef]))
        assertThat mojo.updateJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(equalTo([expectedUpdatedJobDef]))
        assertThat mojo.newSchedules,
                   is(empty())
        assertThat mojo.updatedSchedules,
                   is(empty())
        assertThat mojo.newJobRequests,
                   is(equalTo([expectedJobRequest]))
        assertThat mojo.updatedJobRequests,
                   is(empty())
        def childDetails = getJobRequestDetails(mojo,
                                                expectedSchedule,
                                                expectedUpdatedJobDef,
                                                JobRequestType.Child)
        assertThat childDetails.size(),
                   is(equalTo(1))
        def detail = childDetails[0]
        def expectedJobTypeMetadataId = JobDefMapper.getOracleJobDef('http://stuff'.toURL(),
                                                                     'the app',
                                                                     expectedUpdatedJobDef).jobType
        assertThat detail.jobType,
                   is(equalTo(expectedJobTypeMetadataId))
        def parentDetails = getJobRequestDetails(mojo,
                                                 expectedSchedule,
                                                 expectedUpdatedJobDef,
                                                 JobRequestType.Parent
        )
        assertThat parentDetails.size(),
                   is(equalTo(1))
        detail = parentDetails[0]
        assertThat detail.jobType,
                   is(equalTo(expectedJobTypeMetadataId))
    }

    @Test
    void ExistingRequest_ScheduleChanges_UpdatesWithoutError() {
        // arrange
        this.factories[ScheduleFactory] = [SingleScheduleFactory]
        this.factories[JobDefinitionFactory] = [SingleJobDefFactory]
        def originalSchedule = new SingleScheduleFactory().createSchedule()
        def jobDefinition = new SingleJobDefFactory().createJobDefinition()
        DummyFactory.returnThis = new JobRequest(submissionNotes: 'the notes',
                                                 jobDefinition: jobDefinition,
                                                 schedule: originalSchedule)
        this.factories[JobRequestFactory] = [DummyFactory]
        def mojo = getJobScheduleMojo()
        mojo.execute()
        this.factories[ScheduleFactory] = [UpdatedScheduleFactory]
        def expectedUpdatedSchedule = new UpdatedScheduleFactory().createSchedule()
        DummyFactory.returnThis = new JobRequest(submissionNotes: 'the notes',
                                                 jobDefinition: jobDefinition,
                                                 schedule: expectedUpdatedSchedule)
        mojo = getJobScheduleMojo()

        // act
        println 'issuing 2nd execute call for update'
        mojo.execute()

        // assert
        def expectedJobRequest = DummyFactory.returnThis
        assertThat mojo.newJobDefs,
                   is(empty())
        assertThat mojo.updateJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(empty())
        assertThat mojo.newSchedules,
                   is(empty())
        assertThat mojo.updatedSchedules,
                   is(equalTo([expectedUpdatedSchedule]))
        assertThat mojo.newJobRequests,
                   is(empty())
        assertThat mojo.updatedJobRequests,
                   is(equalTo([expectedJobRequest]))
        def childDetails = getJobRequestDetails(mojo,
                                                expectedUpdatedSchedule,
                                                jobDefinition,
                                                JobRequestType.Child)
        assertThat childDetails.size(),
                   is(equalTo(1))
        def detail = childDetails[0]
        // see updated schedule, orig schedule ran on 1/1
        assertThat new LocalDateTime(detail.scheduledTime).toString(),
                   is(equalTo('2025-01-02T08:15:10.000'))
    }

    int mapJobRequestType(JobRequestType jobRequestType) {
        switch (jobRequestType) {
            case JobRequestType.Child:
                return 3
            case JobRequestType.Parent:
                return 2
            default:
                throw new Exception("Unmapped request type ${jobRequestType}")
        }
    }

    List<RequestDetail> getJobRequestDetails(JobScheduleMojo mojo,
                                             RecurringSchedule expectedSchedule,
                                             JobDefinition expectedJobDef,
                                             JobRequestType jobRequestType) {
        def expectedRequestType = mapJobRequestType jobRequestType
        List<RequestDetail> results = null
        mojo.withContext {
            mojo.withDeployerTransaction {
                MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
                    def jobDefId = metadataWrapper.getJobDefId(expectedJobDef.name).toString()
                    def jobDefFilter = new Filter(RuntimeService.QueryField.DEFINITION.fieldName(),
                                                  Filter.Comparator.EQUALS,
                                                  jobDefId)
                    def requestTypeFilter = new Filter(RuntimeService.QueryField.REQUESTTYPE.fieldName(),
                                                       Filter.Comparator.EQUALS,
                                                       expectedRequestType)
                    def stateFilter = new Filter(RuntimeService.QueryField.STATE.fieldName(),
                                                 Filter.Comparator.NOT_EQUALS,
                                                 State.CANCELLED.value())
                    def combinedFilter = jobDefFilter & requestTypeFilter & stateFilter
                    def runtimeService = runtimeWrapper.runtimeService
                    def requestIds = runtimeService.queryRequests(runtimeWrapper.handle,
                                                                  combinedFilter,
                                                                  null,
                                                                  true)
                    results = requestIds.collect { long requestId ->
                        runtimeService.getRequestDetail(runtimeWrapper.handle, requestId, true)
                    }
            }
        }
        // can't reliably query by schedule name so do it after we get results back
        results.findAll { details ->
            details.schedule.name == expectedSchedule.name
        }
    }
}
