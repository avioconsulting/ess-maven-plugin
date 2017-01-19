package com.avioconsulting.ess

class JobDefinition {
    enum Types {
        SyncWebserviceJobType,
        AsyncWebserviceJobType,
        OnewayWebserviceJobType
    }

    final Types jobType
    final String description
    final URI wsdlUrl
    final String service
    final String port
    final String operation
    final String message

    JobDefinition(Types jobType,
                  String description,
                  URI wsdlUrl,
                  String service,
                  String port,
                  String operation,
                  String message) {

        this.jobType = jobType
        this.description = description
        this.wsdlUrl = wsdlUrl
        this.service = service
        this.port = port
        this.operation = operation
        this.message = message
    }
}
