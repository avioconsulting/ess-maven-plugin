package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.models.JobDefinition
import oracle.as.scheduler.Filter
import oracle.as.scheduler.MetadataService
import oracle.as.scheduler.MetadataServiceHandle

class JobDefDeployer {
    private final String hostingApplication
    private final URL soaUrl
    private final MetadataService service
    private final MetadataServiceHandle handle

    JobDefDeployer(MetadataService service, MetadataServiceHandle handle, String hostingApplication, URL soaUrl) {
        this.handle = handle
        this.service = service
        this.soaUrl = soaUrl
        this.hostingApplication = hostingApplication
    }

    List<String> getExistingDefinitions() {
        def result = this.service.queryJobDefinitions(this.handle,
                                                      new Filter('name',
                                                                 Filter.Comparator.NOT_EQUALS,
                                                                 ''),
                                                      MetadataService.QueryField.NAME,
                                                      true)
        result.collect { id -> id.namePart }
    }

    def createDefinition(JobDefinition definition) {
        def packageName = null
        this.service.addJobDefinition(this.handle, jobDev, packageName)
        doJobDef('CREATE', definition)
    }

    def updateDefinition(JobDefinition definition) {
        doJobDef('UPDATE', definition)
    }
}
