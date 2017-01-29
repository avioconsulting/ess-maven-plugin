package com.avioconsulting.ess.wrappers

import com.avioconsulting.EssPolicyNotifier
import com.avioconsulting.ess.models.EssClientPolicySubject
import com.avioconsulting.ess.models.Policy
import com.avioconsulting.ess.models.PolicySubject
import com.avioconsulting.util.Logger
import com.avioconsulting.util.PythonCaller
import org.python.core.PyString

import javax.naming.InitialContext
import java.util.regex.Pattern

class WsmWrapper {
    private static final List<String> PROPS_THAT_ARE_NOT_OVERRIDES = ['URI', 'category', 'enabled', 'index']
    private final PythonCaller caller
    private final String domainName
    private PolicySubject currentSubject
    private final Logger logger

    WsmWrapper(String adminServerUrl,
               String weblogicUser,
               String weblogicPassword,
               Logger logger) {
        this.logger = logger
        this.currentSubject = null
        def caller = this.caller = new PythonCaller()
        caller.methodCall('connect', [
                url     : adminServerUrl,
                username: weblogicUser,
                password: weblogicPassword
        ])
        this.domainName = ((PyString) caller.cmoGet('name')).toString()
    }

    def begin() {
        caller.methodCall('beginWSMSession')
    }

    def commit() {
        caller.methodCall('commitWSMSession')
    }

    def close() {
        def caller = this.caller
        caller.methodCall('disconnect')
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
        if (!doSelect(policySubject)) {
            assert policySubject instanceof EssClientPolicySubject
            createPolicyAssembly(policySubject)
            // try and avoid potential transaction issues by starting over
            caller.methodCall('abortWSMSession')
            begin()
            if (!doSelect(policySubject)) {
                throw new Exception('Could not select WSM subject, tried forcing job assembly policy to be created and still could not select it!')
            }
        }
        this.currentSubject = policySubject
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

    static void createPolicyAssembly(EssClientPolicySubject subject) {
        // TODO: Don't hard code this stuff
        Hashtable<String, String> props = [
                'java.naming.factory.initial'     : 'weblogic.jndi.WLInitialContextFactory',
                'java.naming.provider.url'        : 't3://localhost:8001',
                'java.naming.security.principal'  : 'weblogic',
                'java.naming.security.credentials': 'oracle1234'
        ]
        def context = new InitialContext(props)
        EssPolicyNotifier notifier = context.lookup(
                'java:global/ess-policy-notifier/EssPolicyNotifierModule/EssPolicyNotifierBean') as EssPolicyNotifier
        def logMessages = notifier.createPolicyAssembly(subject.essHostApplicationName,
                                                        MetadataServiceWrapper.PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                        subject.jobDefinition.name)
        logMessages.each { msg ->
            this.logger.info "From server: ${msg}"
        }
        context.close()
    }

    static List<Policy> parseExistingPolicies(String output) {
        println "paesing existing policies ${output}"
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
