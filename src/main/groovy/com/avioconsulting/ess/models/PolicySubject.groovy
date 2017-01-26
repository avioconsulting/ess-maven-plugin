package com.avioconsulting.ess.models

import groovy.transform.InheritConstructors

@InheritConstructors
abstract class PolicySubject extends BaseModel {
    String getApplication(String domainName) {
        def parts = [
                'WLS',
                domainName,
                this.applicationNameOnly
        ]
        '/' + parts.join('/')
    }

    abstract String getApplicationNameOnly()
    abstract String getAssembly()
    abstract String getSubject()
}
