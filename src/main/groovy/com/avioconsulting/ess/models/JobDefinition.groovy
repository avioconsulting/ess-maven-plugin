package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class JobDefinition extends BaseModel {
    enum Types {
        SyncWebserviceJobType,
        AsyncWebserviceJobType,
        OnewayWebserviceJobType
    }

    String name, description, wsdlPath, service, port, operation, message
    Types jobType
}
