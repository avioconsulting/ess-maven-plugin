package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.models.JobRequest
import com.avioconsulting.ess.models.JobRequestMetadata
import oracle.as.scheduler.*
import org.joda.time.DateTime

class RuntimeWrapper {
    private final RuntimeService runtimeService
    private final RuntimeServiceHandle serviceHandle
    private final MetadataWrapper metadataDeployer
    // should result in everything being returned
    private static final Filter everythingFilter = null
    private static final long NO_PARENTS = -1
    private final Closure logger

    RuntimeWrapper(RuntimeService runtimeService,
                   RuntimeServiceHandle serviceHandle,
                   MetadataWrapper metadataDeployer,
                   Closure logger) {

        this.logger = logger
        this.metadataDeployer = metadataDeployer
        this.runtimeService = runtimeService
        this.serviceHandle = serviceHandle
    }

    def cancelAllRequests() {
        def existing = getRawRequestDetails()
        // cancel any parents first
        existing.findAll {
            details -> details.parent == NO_PARENTS && ![State.CANCELLED, State.CANCELLING].contains(details.state)
        }.each { details ->
            this.logger "Canceling request ID ${details.requestId}"
            this.runtimeService.cancelRequest(this.serviceHandle, details.requestId)
        }
    }

    def deleteAllRequests() {
        def existing = getRawRequestDetails()
        existing.each { details ->
            this.logger "Deleting request ID ${details.requestId}"
            this.runtimeService.deleteRequest(this.serviceHandle, details.requestId)
        }
    }

    List<JobRequestMetadata> getExistingJobRequests() {
        RequestDetail[] requestDetails = getRawRequestDetails()
        // multiple filter values
        requestDetails.findAll {
            details -> ![State.CANCELLED, State.CANCELLING].contains(details.state) && details.parent == NO_PARENTS
        }.collect { details ->
            new JobRequestMetadata(jobRequestName: details.jobDefn.namePart,
                                   scheduleName: details.scheduleDefn.namePart,
                                   id: details.requestId)
        }
    }

    private RequestDetail[] getRawRequestDetails() {
        def requestIds = this.runtimeService.queryRequests(this.serviceHandle,
                                                           everythingFilter,
                                                           null,
                                                           true)
        def idList = requestIds.toList()
        // doesn't like empty array
        if (!idList.any()) {
            return []
        }
        this.runtimeService.getRequestDetails(this.serviceHandle,
                                              (long[]) idList.toArray())
    }

    def createRequest(JobRequest request) {
        def jobDefId = MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_DEFINITION,
                                                               MetadataWrapper.PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                               request.jobDefinition.name)
        def schedule = this.metadataDeployer.getOracleSchedule(request.schedule)
        // using schedules not triggers
        def trigger = null
        def recurrence = schedule.recurrence
        // requests created in EM had today as a start date
        def startDate = new DateTime().toCalendar(Locale.default)
        // requests created in EM had no end date on them
        def endDate = null
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
