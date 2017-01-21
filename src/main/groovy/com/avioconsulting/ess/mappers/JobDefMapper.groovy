package com.avioconsulting.ess.mappers

import oracle.as.scheduler.JobDefinition
import oracle.as.scheduler.MetadataObjectId
import oracle.as.scheduler.ParameterList

class JobDefMapper {
    public static final String JOB_TYPE_PACKAGE_FROM_EM = '/oracle/as/ess/core/'
    private static final Map<com.avioconsulting.ess.models.JobDefinition.Types, String> typeMapping = [
            (com.avioconsulting.ess.models.JobDefinition.Types.SyncWebService)  : 'SyncWebserviceJobType',
            (com.avioconsulting.ess.models.JobDefinition.Types.AsyncWebService) : 'AsyncWebserviceJobType',
            (com.avioconsulting.ess.models.JobDefinition.Types.OneWayWebService): 'OnewayWebserviceJobType'
    ]

    static JobDefinition getOracleJobDef(URL soaUrl,
                                         String hostingApp,
                                         com.avioconsulting.ess.models.JobDefinition jobDefinition) {
        def jobTypeId = MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_TYPE,
                                                                JOB_TYPE_PACKAGE_FROM_EM,
                                                                typeMapping[jobDefinition.jobType])
        def params = new ParameterList()
        getProperties(soaUrl, hostingApp, jobDefinition).each { key, value ->
            params.add key, value, false
        }
        def oracleDefinition = new JobDefinition(jobDefinition.name,
                                                 jobTypeId,
                                                 jobDefinition.description,
                                                 params)
        oracleDefinition.validate()
        oracleDefinition
    }

    private static getProperties(URL soaUrl, String hostingApp,
                                 com.avioconsulting.ess.models.JobDefinition jobDefinition) {
        // these property names come from the web service template in EM
        [
                SYS_effectiveApplication: hostingApp,
                SYS_EXT_wsWsdlBaseUrl   : soaUrl.toString(),
                SYS_EXT_wsWsdlUrl       : jobDefinition.wsdlPath,
                SYS_EXT_wsServiceName   : jobDefinition.service,
                SYS_EXT_wsPortName      : jobDefinition.port,
                SYS_EXT_wsOperationName : jobDefinition.operation,
                SYS_EXT_invokeMessage   : jobDefinition.message,
                SYS_externalJobType     : 'SOA'
        ]
    }
}
