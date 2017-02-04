package com.avioconsulting.ess.models

import com.avioconsulting.ess.wrappers.MetadataServiceWrapper
import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class EssClientPolicySubject extends PolicySubject {
    static final String DEFAULT_ESS_HOST_APP = 'EssNativeHostingApp'
    String essHostApplicationName
    JobDefinition jobDefinition
    String essDeployPackage

    EssClientPolicySubject(Map map) {
        super(map << [essHostApplicationName: (map.essHostApplicationName ?: DEFAULT_ESS_HOST_APP),
                      essDeployPackage      : (map.essDeployPackage ?: MetadataServiceWrapper.DEFAULT_ESS_DEPLOY_PACKAGE)])
    }

    String getApplicationNameOnly() {
        essHostApplicationName
    }

    String getAssembly() {
        '%WsmPolicy:/' + this.essDeployPackage + this.jobDefinition.name
    }

    String getSubject() {
        // the ones created via EM use this
        'Job-Invoke()'
    }
}
