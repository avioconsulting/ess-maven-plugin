package com.avioconsulting.ess.wrappers

import com.avioconsulting.ess.models.JobRequest
import com.avioconsulting.ess.models.JobRequestMetadata
import com.avioconsulting.util.Logger
import oracle.as.scheduler.*
import org.joda.time.DateTime

class RuntimeServiceWrapper {
    private final RuntimeService runtimeService
    private final RuntimeServiceHandle handle
    private final MetadataServiceWrapper metadataWrapper
    // should result in everything being returned
    private static final Filter everythingFilter = null
    private static final long NO_PARENTS = -1
    private final Logger logger
    private final boolean holdRequests

    RuntimeServiceWrapper(RuntimeService runtimeService,
                          RuntimeServiceHandle handle,
                          MetadataServiceWrapper metadataDeployer,
                          Logger logger,
                          boolean holdRequests) {

        this.holdRequests = holdRequests
        this.logger = logger
        this.metadataWrapper = metadataDeployer
        this.runtimeService = runtimeService
        this.handle = handle
    }

    def cancelAllRequests() {
        def idList = getRequestIds()
        // getRequestDetails doesn't like empty calls
        if (!idList.any()) {
            return
        }
        def existing = this.runtimeService.getRequestDetails(this.handle,
                                                             (long[]) idList.toArray())
        // cancel any parents first
        existing.findAll {
            details -> details.parent == NO_PARENTS && ![State.CANCELLED, State.CANCELLING].contains(details.state)
        }.each { details ->
            this.logger.info "Canceling request ID ${details.requestId}"
            this.runtimeService.cancelRequest(this.handle, details.requestId)
        }
    }

    def deleteAllRequests() {
        def idList = getRequestIds()
        idList.each { id ->
            this.logger.info "Deleting request ID ${id}"
            this.runtimeService.deleteRequest(this.handle, id)
        }
    }

    List<JobRequestMetadata> getExistingJobRequests(boolean includeCancelled = false) {
        def idList = getRequestIds()
        idList.collect { id ->
            // for some reason, the job requests we create via this API only return the schedule if we fetch
            // them individually. EM created requests do not have that problem
            this.runtimeService.getRequestDetail(this.handle, id, true)
        }.findAll { details ->
            // we only need to update/work with parent requests
            (includeCancelled || ![State.CANCELLED, State.CANCELLING].contains(
                    details.state)) && details.parent == NO_PARENTS
        }.collect { details ->
            new JobRequestMetadata(jobDefinitionName: details.jobDefn.namePart,
                                   scheduleName: details.schedule.name,
                                   id: details.requestId)
        }
    }

    def cancelRequestsFor(com.avioconsulting.ess.models.JobDefinition definition) {
        def existingRequests = getExistingJobRequests()
        existingRequests.findAll { metadata -> metadata.jobDefinitionName == definition.name }
                .each { ourRequest ->
            this.logger.info "Cancelling job request ID ${ourRequest.id}..."
            this.runtimeService.cancelRequest(this.handle, ourRequest.id)
        }
    }


    def deleteRequestsFor(com.avioconsulting.ess.models.JobDefinition definition) {
        def existingRequests = getExistingJobRequests(true)
        existingRequests.findAll { metadata -> metadata.jobDefinitionName == definition.name }
                .each { ourRequest ->
            this.logger.info "Deleting job request ID ${ourRequest.id}..."
            this.runtimeService.deleteRequest(this.handle, ourRequest.id)
        }
    }

    private List getRequestIds() {
        def requestIds = this.runtimeService.queryRequests(this.handle,
                                                           everythingFilter,
                                                           null,
                                                           true)
        requestIds.toList()
    }

    def updateRequest(JobRequestMetadata metadata) {
        // parameters from an updated job definition don't seem to make it in unless we explicitly update
        def jobDef = this.metadataWrapper.getOracleJobDefinition(metadata.jobDefinitionName)
        this.logger.info 'Updating parameters on existing job request from job definition...'
        def runtimeService = this.runtimeService
        def jobRequestId = metadata.id
        jobDef.parameters.all.each { param ->
            runtimeService.updateRequestParameter(this.handle,
                                                  jobRequestId,
                                                  param.name,
                                                  param.value)
        }
        this.logger.info "Pointing job request ${jobRequestId} at newly updated schedule..."
        // updating schedule creates a new 'pending' job request for the next date
        // this has to happen after we update the parameters from the job definition above
        def scheduleId = MetadataServiceWrapper.getScheduleId(metadata.scheduleName)
        runtimeService.replaceSchedule(this.handle, jobRequestId, scheduleId)
        def currentState = runtimeService.getRequestState(this.handle, jobRequestId)
        if (this.holdRequests && currentState != State.HOLD) {
            this.logger.info 'Moving request to hold status'
            runtimeService.holdRequest(this.handle, jobRequestId)
        } else if (!this.holdRequests && currentState == State.HOLD) {
            this.logger.info 'Releasing request from hold'
            runtimeService.releaseRequest(this.handle, jobRequestId)
        }
    }

    def createRequest(JobRequest request) {
        def jobDefId = MetadataServiceWrapper.getJobDefId request.jobDefinition.name
        def schedule = this.metadataWrapper.getOracleSchedule(request.schedule)
        // using schedules not triggers
        def trigger = null
        // requests created in EM had today as a start date
        def startDate = new DateTime().toCalendar(Locale.default)
        // requests created in EM had no end date on them
        def endDate = null
        def params = new RequestParameters()
        def runtimeService = this.runtimeService
        def requestId = runtimeService.submitRequest(this.handle,
                                                     request.submissionNotes,
                                                     jobDefId,
                                                     schedule,
                                                     trigger,
                                                     startDate,
                                                     endDate,
                                                     params)
        this.logger.info "Request ${requestId} created..."
        if (this.holdRequests) {
            this.logger.info 'Putting request in a HOLD state'
            runtimeService.holdRequest(this.handle, requestId)
        }
    }
}
