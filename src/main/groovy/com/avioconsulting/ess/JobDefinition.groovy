package com.avioconsulting.ess

class JobDefinition {
    enum Types {
        SyncWebserviceJobType,
        AsyncWebserviceJobType,
        OnewayWebserviceJobType
    }

    final String name
    final Types jobType
    final String description
    final URL wsdlUrl
    final String service
    final String port
    final String operation
    final String message

    JobDefinition(Types jobType, String description, URL wsdlUrl, String service, String port, String operation, String message, String name) {

        this.jobType = jobType
        this.description = description
        this.wsdlUrl = wsdlUrl
        this.service = service
        this.port = port
        this.operation = operation
        this.message = message
        this.name = name
    }
}
