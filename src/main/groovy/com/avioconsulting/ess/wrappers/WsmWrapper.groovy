package com.avioconsulting.ess.wrappers

import com.avioconsulting.ess.models.Policy
import com.avioconsulting.ess.models.PolicySubject
import com.avioconsulting.util.PythonCaller
import org.python.core.PyString

class WsmWrapper {
    private final PythonCaller caller
    private final String domainName

    WsmWrapper(String adminServerUrl, String weblogicUser, String weblogicPassword) {
        def caller = this.caller = new PythonCaller()
        caller.methodCall('connect', [
                url     : adminServerUrl,
                username: weblogicUser,
                password: weblogicPassword
        ])
        this.domainName = ((PyString) caller.cmoGet('name')).toString()
        caller.methodCall('beginWSMSession')
    }

    def close() {
        this.caller.methodCall('commitWSMSession')
        this.caller.methodCall('disconnect')
    }

    List<Policy> getExistingPolicies(PolicySubject policySubject) {

    }
}
