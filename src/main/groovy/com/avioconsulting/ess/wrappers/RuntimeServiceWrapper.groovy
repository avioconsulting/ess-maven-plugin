package com.avioconsulting.ess.wrappers

import com.avioconsulting.ess.models.JobRequest
import com.avioconsulting.ess.models.JobRequestMetadata
import com.avioconsulting.ess.models.RecurringSchedule
import com.avioconsulting.util.Logger
import oracle.as.scheduler.*
import org.joda.time.DateTime

class RuntimeServiceWrapper {
    private final RuntimeService runtimeService
    private final RuntimeServiceHandle handle
    private final MetadataServiceWrapper metadataWrapper
    private static final long NO_PARENTS = -1
    private final Logger logger
    private final boolean holdRequests
    // should result in everything being returned
    private static final Filter everythingFilter = null
    private static final Filter notCancellingFilter = new Filter(RuntimeService.QueryField.STATE.fieldName(),
                                                                 Filter.Comparator.NOT_EQUALS,
                                                                 State.CANCELLING.value())
    private static final Filter notCancelledFilter = new Filter(RuntimeService.QueryField.STATE.fieldName(),
                                                                Filter.Comparator.NOT_EQUALS,
                                                                State.CANCELLED.value())
    private static final Filter notCancelCombinedFilter = notCancellingFilter & notCancelledFilter
    private static final int REQUEST_TYPE_PARENT = 2

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
        def idList = getRequestIds(false)
        // getRequestDetails doesn't like empty calls
        if (!idList.any()) {
            return
        }
        def existing = this.runtimeService.getRequestDetails(this.handle,
                                                             idList.toArray() as long[])
        // cancel any parents first
        existing.findAll { details -> details.parent == NO_PARENTS }.each { details ->
            this.logger.info "Canceling request ID ${details.requestId}"
            this.runtimeService.cancelRequest(this.handle, details.requestId)
        }
    }

    def deleteAllRequests() {
        def idList = getRequestIds(true)
        idList.each { id ->
            this.logger.info "Deleting request ID ${id}"
            this.runtimeService.deleteRequest(this.handle, id)
        }
    }

    def cancelRequestsFor(com.avioconsulting.ess.models.JobDefinition definition) {
        def existingRequests = getExistingJobRequests(definition)
        existingRequests.each { ourRequest ->
            this.logger.info "Cancelling job request ID ${ourRequest.id}..."
            this.runtimeService.cancelRequest(this.handle, ourRequest.id)
        }
    }

    def deleteRequestsFor(com.avioconsulting.ess.models.JobDefinition definition) {
        def existingRequests = getExistingJobRequests(definition,
                                                      null,
                                                      true)
        existingRequests.each { ourRequest ->
            this.logger.info "Deleting job request ID ${ourRequest.id}..."
            this.runtimeService.deleteRequest(this.handle, ourRequest.id)
        }
    }

    // will retrieve for all schedules if schedule == null
    List<JobRequestMetadata> getExistingJobRequests(com.avioconsulting.ess.models.JobDefinition definition,
                                                    RecurringSchedule schedule = null,
                                                    boolean includeCancelled = false) {
        def jobDefId = metadataWrapper.getJobDefId(definition.name).toString()
        def jobDefFilter = new Filter(RuntimeService.QueryField.DEFINITION.fieldName(),
                                      Filter.Comparator.EQUALS,
                                      jobDefId)
        def requestTypeFilter = new Filter(RuntimeService.QueryField.REQUESTTYPE.fieldName(),
                                           Filter.Comparator.EQUALS,
                                           REQUEST_TYPE_PARENT)
        def combinedFilter = jobDefFilter & requestTypeFilter
        if (!includeCancelled) {
            combinedFilter = combinedFilter & notCancelCombinedFilter
        }
        def requestIds = runtimeService.queryRequests(this.handle,
                                                      combinedFilter,
                                                      null,
                                                      true)
        requestIds.collect { long requestId ->
            runtimeService.getRequestDetail(this.handle, requestId, true)
        }.findAll { details ->
            // can't reliably query by schedule name so do it after we get results back
            !schedule || (details.schedule.name == schedule.name)
        }.collect { details ->
            new JobRequestMetadata(jobDefinitionName: details.jobDefn.namePart,
                                   scheduleName: details.schedule.name,
                                   id: details.requestId)
        }
    }

    private List getRequestIds(boolean includeCancelled) {
        def filter = includeCancelled ? everythingFilter : notCancelCombinedFilter
        def requestIds = this.runtimeService.queryRequests(this.handle,
                                                           filter,
                                                           null,
                                                           true)
        requestIds.toList()
    }

    def updateRequest(JobRequestMetadata metadata) {
        // parameters from an updated job definition don't seem to make it in unless we explicitly update
        def metadataWrapper = this.metadataWrapper
        def jobDef = metadataWrapper.getOracleJobDefinition(metadata.jobDefinitionName)
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
        def scheduleId = metadataWrapper.getScheduleId(metadata.scheduleName)
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
        def metadataWrapper = this.metadataWrapper
        def jobDefId = metadataWrapper.getJobDefId request.jobDefinition.name
        def schedule = metadataWrapper.getOracleSchedule(request.schedule)
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
