package com.avioconsulting.ess.wrappers

import com.avioconsulting.ess.models.Policy
import com.avioconsulting.ess.models.PolicySubject
import com.avioconsulting.util.PythonCaller
import org.python.core.PyString

import java.util.regex.Pattern

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
        def caller = this.caller
        caller.methodCall('commitWSMSession')
        caller.methodCall('disconnect')
    }

    def attachPolicy(PolicySubject policySubject, Policy policy) {
        selectSubject(policySubject)
        this.caller.methodCall 'attachWSMPolicy',
                               [policy.name]
    }

    def detachPolicy(PolicySubject policySubject, Policy policy) {
        selectSubject(policySubject)
        this.caller.methodCall 'detachWSMPolicy',
                               [policy.name]
    }

    List<Policy> getExistingPolicies(PolicySubject policySubject) {
        selectSubject(policySubject)
        def caller = this.caller
        def output = caller.withInterceptedStdout {
            caller.methodCall('displayWSMPolicySet')
        }
        parseExistingPolicies output
    }

    private selectSubject(PolicySubject policySubject) {
        this.caller.methodCall 'selectWSMPolicySubject',
                               [
                                       application: policySubject.getApplication(this.domainName),
                                       assembly   : policySubject.assembly,
                                       subject    : policySubject.subject
                               ]
    }

    static List<Policy> parseExistingPolicies(String output) {
        int flags = Pattern.DOTALL | Pattern.MULTILINE
        def matcher = new Pattern(/.*Policy Reference:\s+(.*)/, flags).matcher(output)
        assert matcher.matches()
        def uriList = matcher.group(1)
        matcher = new Pattern(/URI=(\S+),/, flags).matcher(uriList)
        matcher.collect { m ->
            new Policy(name: m[1])
        }
    }
}
