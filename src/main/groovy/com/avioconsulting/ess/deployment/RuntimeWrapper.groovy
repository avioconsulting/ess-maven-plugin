package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.models.JobRequest
import oracle.as.scheduler.*

class RuntimeWrapper {
    private final RuntimeService runtimeService
    private final RuntimeServiceHandle serviceHandle
    private final MetadataWrapper metadataDeployer

    RuntimeWrapper(RuntimeService runtimeService,
                   RuntimeServiceHandle serviceHandle,
                   MetadataWrapper metadataDeployer) {

        this.metadataDeployer = metadataDeployer
        this.runtimeService = runtimeService
        this.serviceHandle = serviceHandle
    }

    def doesJobRequestExist(JobRequest request) {
        def results = this.runtimeService.queryRequests(this.serviceHandle,
                                                        new Filter(RuntimeService.QueryField.SCHEDULE.fieldName(),
                                                                   Filter.Comparator.EQUALS,
                                                                   request.schedule.displayName),
                                                        RuntimeService.QueryField.SCHEDULE,
                                                        true)
        results.each { id ->
            println "exist job request with id ${id}"
        }
        results.any()
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
