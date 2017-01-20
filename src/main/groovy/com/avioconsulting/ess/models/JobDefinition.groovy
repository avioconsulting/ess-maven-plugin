package com.avioconsulting.ess.models

class JobDefinition {
    enum Types {
        SyncWebserviceJobType,
        AsyncWebserviceJobType,
        OnewayWebserviceJobType
    }

    final String name
    final Types jobType
    final String description
    final String wsdlPath
    final String service
    final String port
    final String operation
    final String message

    JobDefinition(Map params) {
        this.jobType = params.jobType
        this.description = params.description
        this.wsdlPath = params.wsdlPath
        this.service = params.service
        this.port = params.port
        this.operation = params.operation
        this.message = params.message
        this.name = params.name
    }
}
