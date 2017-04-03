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
    private static final String WSDL_PATH_PROP = 'SYS_EXT_wsWsdlUrl'
    private static final String WSDL_SERVICE = 'SYS_EXT_wsServiceName'
    private static final String WSDL_PORT = 'SYS_EXT_wsPortName'
    private static final String WSDL_OPERATION = 'SYS_EXT_wsOperationName'
    private static final String WSDL_MESSAGE = 'SYS_EXT_invokeMessage'

    private static
    final Map<String, com.avioconsulting.ess.models.JobDefinition.Types> reverseTypeMapping = typeMapping.collectEntries {
        ourType, theirType ->
            [(theirType): ourType]
    }

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

    static com.avioconsulting.ess.models.JobDefinition getAvioJobDefinition(JobDefinition oracleDefinition) {
        def avioJobType = reverseTypeMapping[oracleDefinition.jobType.namePart]
        def paramValue = { String param ->
            oracleDefinition.parameters.get(param).value as String
        }
        new com.avioconsulting.ess.models.JobDefinition(jobType: avioJobType,
                                                        description: oracleDefinition.description,
                                                        wsdlPath: paramValue(WSDL_PATH_PROP),
                                                        service: paramValue(WSDL_SERVICE),
                                                        port: paramValue(WSDL_PORT),
                                                        operation: paramValue(WSDL_OPERATION),
                                                        message: paramValue(WSDL_MESSAGE),
                                                        name: oracleDefinition.name)
    }

    private static getProperties(URL soaUrl,
                                 String hostingApp,
                                 com.avioconsulting.ess.models.JobDefinition jobDefinition) {
        // these property keys come from the web service template in EM
        [
                SYS_effectiveApplication: hostingApp,
                SYS_EXT_wsWsdlBaseUrl   : soaUrl.toString(),
                (WSDL_PATH_PROP)        : jobDefinition.wsdlPath,
                (WSDL_SERVICE)          : jobDefinition.service,
                (WSDL_PORT)             : jobDefinition.port,
                (WSDL_OPERATION)        : jobDefinition.operation,
                (WSDL_MESSAGE)          : jobDefinition.message,
                SYS_externalJobType     : 'SOA'
        ]
    }
}
