package com.avioconsulting.ess.mappers

import oracle.as.scheduler.JobDefinition
import oracle.as.scheduler.MetadataObjectId
import oracle.as.scheduler.ParameterList

class JobDefMapper {
    static JobDefinition getOracleJobDef(URL soaUrl, com.avioconsulting.ess.models.JobDefinition jobDefinition) {
        // TODO: get the correct package name through the wizard
        def id = MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_DEFINITION,
                                                         '/oracle/as',
                                                         jobDefinition.name)
        def params = new ParameterList()
        getProperties(soaUrl, jobDefinition).each {key, value ->
            params.add key, value, false
        }
        return new JobDefinition(jobDefinition.name,
                                 id,
                                 jobDefinition.description,
                                 params)
    }

    private static getProperties(URL soaUrl, com.avioconsulting.ess.models.JobDefinition jobDefinition) {
        // these property names come from the web service template in EM
        [
                // TODO: is this populated by the system if we don't include it??
                SYS_effectiveApplication: 'EssNativeHostingApp',
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
