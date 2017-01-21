package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class JobRequestMetadata extends BaseModel {
    long id
    String jobDefinitionName
    String scheduleName
}
