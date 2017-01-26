package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class PolicyAttachment extends BaseModel {
    List<Policy> policies
    PolicySubject policySubject

    PolicyAttachment(Map map) {
        super(map)
    }
}
