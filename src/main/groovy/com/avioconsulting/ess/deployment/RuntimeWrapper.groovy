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
        def idList = getRequestIds()
        // getRequestDetails doesn't like empty calls
        if (!idList.any()) {
            return
        }
        def existing = this.runtimeService.getRequestDetails(this.serviceHandle,
                                                             (long[]) idList.toArray())
        // cancel any parents first
        existing.findAll {
            details -> details.parent == NO_PARENTS && ![State.CANCELLED, State.CANCELLING].contains(details.state)
        }.each { details ->
            this.logger "Canceling request ID ${details.requestId}"
            this.runtimeService.cancelRequest(this.serviceHandle, details.requestId)
        }
    }

    def deleteAllRequests() {
        def idList = getRequestIds()
        idList.each { id ->
            this.logger "Deleting request ID ${id}"
            this.runtimeService.deleteRequest(this.serviceHandle, id)
        }
    }

    List<JobRequestMetadata> getExistingJobRequests() {
        def idList = getRequestIds()
        idList.collect { id ->
            // for some reason, the job requests we create via this API only return the schedule if we fetch
            // them individually. EM created requests do not have that problem
            this.runtimeService.getRequestDetail(this.serviceHandle, id, true)
        }.findAll { details ->
            // we only need to update/work with parent requests
            ![State.CANCELLED, State.CANCELLING].contains(details.state) && details.parent == NO_PARENTS
        }.collect { details ->
            new JobRequestMetadata(jobDefinitionName: details.jobDefn.namePart,
                                   scheduleName: details.schedule.name,
                                   id: details.requestId)
        }
    }

    private List getRequestIds() {
        def requestIds = this.runtimeService.queryRequests(this.serviceHandle,
                                                           everythingFilter,
                                                           null,
                                                           true)
        requestIds.toList()
    }

    def updateRequestSchedule(JobRequestMetadata metadata) {
        def scheduleId = MetadataWrapper.getScheduleId(metadata.scheduleName)
        this.runtimeService.replaceSchedule(this.serviceHandle, metadata.id, scheduleId)
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
