package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.models.JobRequest
import com.avioconsulting.ess.models.JobRequestMetadata
import oracle.as.scheduler.*
import org.joda.time.DateTime

class RuntimeServiceWrapper {
    private final RuntimeService runtimeService
    private final RuntimeServiceHandle serviceHandle
    private final MetadataServiceWrapper metadataWrapper
    // should result in everything being returned
    private static final Filter everythingFilter = null
    private static final long NO_PARENTS = -1
    private final Closure logger
    private final boolean holdRequests

    RuntimeServiceWrapper(RuntimeService runtimeService,
                          RuntimeServiceHandle serviceHandle,
                          MetadataServiceWrapper metadataDeployer,
                          Closure logger,
                          boolean holdRequests) {

        this.holdRequests = holdRequests
        this.logger = logger
        this.metadataWrapper = metadataDeployer
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

    def updateRequest(JobRequestMetadata metadata) {
        // parameters from an updated job definition don't seem to make it in unless we explicitly update
        def jobDef = this.metadataWrapper.getOracleJobDefinition(metadata.jobDefinitionName)
        this.logger 'Updating parameters on existing job request from job definition...'
        def runtimeService = this.runtimeService
        def jobRequestId = metadata.id
        jobDef.parameters.all.each { param ->
            runtimeService.updateRequestParameter(this.serviceHandle,
                                                  jobRequestId,
                                                  param.name,
                                                  param.value)
        }
        this.logger "Pointing job request ${jobRequestId} at newly updated schedule..."
        // updating schedule creates a new 'pending' job request for the next date
        // this has to happen after we update the parameters from the job definition above
        def scheduleId = MetadataServiceWrapper.getScheduleId(metadata.scheduleName)
        runtimeService.replaceSchedule(this.serviceHandle, jobRequestId, scheduleId)
        def currentState = runtimeService.getRequestState(this.serviceHandle, jobRequestId)
        if (this.holdRequests && currentState != State.HOLD) {
            this.logger 'Moving request to hold status'
            runtimeService.holdRequest(this.serviceHandle, jobRequestId)
        } else if (!this.holdRequests && currentState == State.HOLD) {
            this.logger 'Releasing request from hold'
            runtimeService.releaseRequest(this.serviceHandle, jobRequestId)
        }
    }

    def createRequest(JobRequest request) {
        def jobDefId = MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_DEFINITION,
                                                               MetadataServiceWrapper.PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                               request.jobDefinition.name)
        def schedule = this.metadataWrapper.getOracleSchedule(request.schedule)
        // using schedules not triggers
        def trigger = null
        // requests created in EM had today as a start date
        def startDate = new DateTime().toCalendar(Locale.default)
        // requests created in EM had no end date on them
        def endDate = null
        def params = new RequestParameters()
        def runtimeService = this.runtimeService
        def requestId = runtimeService.submitRequest(this.serviceHandle,
                                                     request.submissionNotes,
                                                     jobDefId,
                                                     schedule,
                                                     trigger,
                                                     startDate,
                                                     endDate,
                                                     params)
        this.logger "Request ${requestId} created..."
        if (this.holdRequests) {
            this.logger 'Putting request in a HOLD state'
            runtimeService.holdRequest(this.serviceHandle, requestId)
        }
    }
}
