package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.mappers.JobDefMapper
import com.avioconsulting.ess.models.JobDefinition
import oracle.as.scheduler.Filter
import oracle.as.scheduler.MetadataObjectId
import oracle.as.scheduler.MetadataService
import oracle.as.scheduler.MetadataServiceHandle

class ESSDeployer {
    private final String hostingApplication
    private final URL soaUrl
    private final MetadataService service
    private final MetadataServiceHandle handle
    private static final String PACKAGE_NAME_WHEN_CREATED_VIA_EM = '/oracle/apps/ess/custom/'

    ESSDeployer(MetadataService service, MetadataServiceHandle handle, String hostingApplication, URL soaUrl) {
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
        def oracleDef = JobDefMapper.getOracleJobDef(this.soaUrl,
                                                     this.hostingApplication,
                                                     definition)
        this.service.addJobDefinition(this.handle,
                                      oracleDef,
                                      PACKAGE_NAME_WHEN_CREATED_VIA_EM)
    }

    def updateDefinition(JobDefinition definition) {
        def oracleDef = JobDefMapper.getOracleJobDef(this.soaUrl,
                                                     this.hostingApplication,
                                                     definition)
        def id = MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_DEFINITION,
                                                         PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                         definition.name)
        this.service.updateJobDefinition(this.handle,
                                         id,
                                         oracleDef)
    }
}
