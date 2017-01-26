package com.avioconsulting.ess.models

import groovy.transform.InheritConstructors

@InheritConstructors
abstract class PolicySubject extends BaseModel {
    String getApplication(String domainName) {
        throw new Exception('NIE')
    }

    abstract String getApplicationNameOnly()
    abstract String getAssembly()
    abstract String getSubject()
}
