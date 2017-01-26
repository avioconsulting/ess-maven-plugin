package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class EssClientPolicySubject extends PolicySubject {
    public static final String PLACEHOLDER_APP_NAME = 'PLACEHOLDER'
    String essHostApplicationName
    JobDefinition jobDefinition

    EssClientPolicySubject(Map map) {
        super(map << [essHostApplicationName: (map.essHostApplicationName ?: PLACEHOLDER_APP_NAME)])
    }

    String getApplicationNameOnly() {
        return null
    }

    String getAssembly() {
        return null
    }

    String getSubject() {
        return null
    }
}
