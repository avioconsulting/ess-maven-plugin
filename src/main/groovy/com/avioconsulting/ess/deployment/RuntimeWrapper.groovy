package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.models.JobRequest
import com.avioconsulting.ess.models.JobRequestMetadata
import oracle.as.scheduler.*

class RuntimeWrapper {
    private final RuntimeService runtimeService
    private final RuntimeServiceHandle serviceHandle
    private final MetadataWrapper metadataDeployer
    // should result in everything being returned
    private static final Filter everythingFilter = null
    private static final long NO_PARENTS = -1

    RuntimeWrapper(RuntimeService runtimeService,
                   RuntimeServiceHandle serviceHandle,
                   MetadataWrapper metadataDeployer) {

        this.metadataDeployer = metadataDeployer
        this.runtimeService = runtimeService
        this.serviceHandle = serviceHandle
    }

    List<JobRequestMetadata> getExistingJobRequests() {
        def requestIds = this.runtimeService.queryRequests(this.serviceHandle,
                                                           everythingFilter,
                                                           RuntimeService.QueryField.SCHEDULE,
                                                           true)
        def requestDetails = this.runtimeService.getRequestDetails(this.serviceHandle,
                                                                   (long[]) requestIds.toList().toArray())
        // multiple filter values
        requestDetails.findAll {
            details -> ![State.CANCELLED, State.CANCELLING].contains(details.state) && details.parent == NO_PARENTS
        }.collect { details ->
            new JobRequestMetadata(jobRequestName: details.jobDefn.namePart,
                                   scheduleName: details.scheduleDefn.namePart,
                                   id: details.requestId)
        }
        //println 'cancel parent request'
        //this.runtimeService.cancelRequest(this.serviceHandle, 1)
    }

    def createRequest(JobRequest request) {
        def jobDefId = MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_DEFINITION,
                                                               MetadataWrapper.PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                               request.jobDefinition.name)
        def schedule = this.metadataDeployer.getOracleSchedule(request.schedule)
        // using schedules not triggers
        def trigger = null
        def recurrence = schedule.recurrence
        def startDate = recurrence.startDate
        def endDate = recurrence.endDate
        def params = new RequestParameters()
        this.runtimeService.submitRequest(this.serviceHandle,
                                          request.description,
                                          jobDefId,
                                          schedule,
                                          trigger,
                                          startDate,
                                          endDate,
                                          params)
    }
}
