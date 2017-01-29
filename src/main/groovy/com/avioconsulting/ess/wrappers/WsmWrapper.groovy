package com.avioconsulting.ess.wrappers

import com.avioconsulting.ess.models.EssClientPolicySubject
import com.avioconsulting.ess.models.Policy
import com.avioconsulting.ess.models.PolicySubject
import com.avioconsulting.util.EssPolicyFixer
import com.avioconsulting.util.Logger
import com.avioconsulting.util.PythonCaller
import org.python.core.PyString

import java.util.regex.Pattern

class WsmWrapper {
    private final int SELECT_RETRIES = 10
    private static final List<String> PROPS_THAT_ARE_NOT_OVERRIDES = ['URI', 'category', 'enabled', 'index']
    private final PythonCaller caller
    private final String domainName
    private PolicySubject currentSubject
    private final Logger logger
    private final EssPolicyFixer fixer

    WsmWrapper(PythonCaller caller, EssPolicyFixer fixer, Logger logger) {
        this.fixer = fixer
        this.logger = logger
        this.currentSubject = null
        this.caller = caller
        this.domainName = ((PyString) caller.cmoGet('name')).toString()
    }

    def begin() {
        caller.methodCall('beginWSMSession')
    }

    def commit() {
        caller.methodCall('commitWSMSession')
    }

    def attachPolicy(PolicySubject policySubject, Policy policy) {
        selectSubject(policySubject)
        def caller = this.caller
        def policyName = policy.name
        def output = caller.withInterceptedStdout {
            caller.methodCall 'attachWSMPolicy',
                              [policyName]
        }
        if (!output.contains("Policy reference \"${policyName}\" added")) {
            throw new Exception("Unable to attach policy, error: ${output}")
        }
        applyOverrides(policySubject, policy)
    }

    def applyOverrides(PolicySubject policySubject, Policy policy) {
        selectSubject(policySubject)
        def caller = this.caller
        def policyName = policy.name
        policy.overrides.each { key, value ->
            def output = caller.withInterceptedStdout {
                this.logger.info "For policy ${policyName}, setting override ${key} to ${value}..."
                caller.methodCall 'setWSMPolicyOverride',
                                  [policyName,
                                   key,
                                   value]
            }
            parseOverrideOutput output, key
        }
    }

    static private parseOverrideOutput(String output, String key) {
        def flags = Pattern.DOTALL | Pattern.MULTILINE
        if (new Pattern(".*override property \"${key}\".*((added)|(updated)).*",
                        flags).matcher(output).matches()) {
            return true
        }
        throw new Exception("Unable to set override, error: ${output}")
    }

    def detachPolicy(PolicySubject policySubject, Policy policy) {
        selectSubject(policySubject)
        def caller = this.caller
        def policyName = policy.name
        def output = caller.withInterceptedStdout {
            caller.methodCall 'detachWSMPolicy',
                              [policyName]
        }
        if (!output.contains("Policy reference \"${policy.name}\" removed")) {
            throw new Exception("Unable to detach policy, error: ${output}")
        }
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
        if (this.currentSubject == policySubject) {
            return
        }
        selectWithRetries policySubject
        this.currentSubject = policySubject
    }

    def selectWithRetries(PolicySubject policySubject) {
        [1..SELECT_RETRIES][0].find { index ->
            if (doSelect(policySubject)) {
                return true
            }
            assert policySubject instanceof EssClientPolicySubject
            this.logger.info 'Using ESS fixer to correct WSM subject selection...'
            // Creating ESS job definitions programmatically does not leave WSM/ESS in a state where
            // policies can be attached through the traditional WSM WLST calls in this class
            // The EM GUI does something else, which is probably creating a policy assembly
            // We call that here and then retry
            this.fixer.createPolicyAssembly(policySubject.essHostApplicationName,
                                            MetadataServiceWrapper.PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                            policySubject.jobDefinition.name)
            // try and avoid potential transaction issues by starting over
            caller.methodCall('abortWSMSession')
            begin()
            if (!doSelect(policySubject)) {
                if (index == SELECT_RETRIES) {
                    throw new Exception("Tried ${index} times and failed!")
                }
                this.logger.info "Select WSM subject try ${index} failed, sleeping 2 sec"
                Thread.sleep(2000)
                return false
            }
            return true
        }
    }

    private boolean doSelect(PolicySubject policySubject) {
        def output = this.caller.withInterceptedStdout {
            this.caller.methodCall 'selectWSMPolicySubject',
                                   [
                                           application: policySubject.getApplication(this.domainName),
                                           assembly   : policySubject.assembly,
                                           subject    : policySubject.subject
                                   ]
        }
        this.logger.info "Output from selectWSMPolicySubject: ${output}"
        output.contains('policy subject is selected')
    }


    static List<Policy> parseExistingPolicies(String output) {
        int flags = Pattern.DOTALL | Pattern.MULTILINE
        def matcher = new Pattern(/.*Policy Reference:\s+(.*)/, flags).matcher(output)
        assert matcher.matches()
        def uriList = matcher.group(1)
        def keyValues = new Pattern(/(\S+)=(\S+)/, flags).matcher(uriList).collect { match ->
            [match[1], match[2].replace(',', '')]
        }
        def policies = []
        def currentMap = [:]
        def addNewPolicyFromMap = {
            def overrides = currentMap.findAll { key, value ->
                !PROPS_THAT_ARE_NOT_OVERRIDES.contains(key)
            }
            policies << new Policy(name: currentMap.URI,
                                   overrides: overrides)
            currentMap = [:]
        }
        keyValues.each { kvArray ->
            def key = kvArray[0]
            def value = kvArray[1]
            if (currentMap.any() && key == 'URI') {
                addNewPolicyFromMap()
            }
            currentMap[key] = value
        }
        // catch the last one
        addNewPolicyFromMap()
        policies
    }
}
