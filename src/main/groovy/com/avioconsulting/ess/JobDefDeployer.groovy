package com.avioconsulting.ess

import java.util.regex.Pattern

class JobDefDeployer {
    private final String hostingApplication
    private final PythonCaller caller

    JobDefDeployer(PythonCaller caller, String hostingApplication) {
        this.caller = caller
        this.hostingApplication = hostingApplication
    }

    List<String> getExistingDefinitions() {
        def output = this.caller.withInterceptedStdout {
            this.caller.methodCall('manageSchedulerJobDefn',
                                   ['SHOW', this.hostingApplication],
                                   [:])
        }
        return parseDefinitions(output)
    }

    List<String> parseDefinitions(String output) {
        int flags = Pattern.DOTALL | Pattern.MULTILINE
        if (new Pattern(/.*No Job Definitions present.*/, flags).matcher(output).matches()) {
            return []
        }
        def matcher = new Pattern(".*Job Definitions present in namespace of \"${this.hostingApplication}\" are: \$(.*)",
                                  flags).matcher(output)
        assert matcher.matches()
        def listing = matcher.group(1)
        matcher = new Pattern(/(\S+): JobDefinition:\/\S+/, flags).matcher(listing)
        matcher.collect { m -> m[1] }
    }

    def createDefinition(JobDefinition definition) {
        doJobDef('CREATE', definition)
    }

    def updateDefinition(JobDefinition definition) {
        doJobDef('UPDATE', definition)
    }

    private doJobDef(String operation, JobDefinition jobDefinition) {
        this.caller.methodCall('manageSchedulerJobDefn',
                               [operation, this.hostingApplication],
                               [
                                       jobName: jobDefinition.name,
                                       jobType: jobDefinition.jobType.toString(),
                                       desc   : jobDefinition.description,
                                       props  : (getProperties(jobDefinition) as HashMap<String, String>)
                               ])
    }

    def getProperties(JobDefinition jobDefinition) {
        def wsdlUrl = jobDefinition.wsdlUrl
        def baseUrl = wsdlUrl.toString().replace(wsdlUrl.path, '')
        [
                SYS_effectiveApplication: this.hostingApplication,
                SYS_EXT_wsWsdlBaseUrl   : baseUrl,
                SYS_EXT_wsWsdlUrl       : wsdlUrl.path,
                SYS_EXT_wsServiceName   : jobDefinition.service,
                SYS_EXT_wsPortName      : jobDefinition.port,
                SYS_EXT_wsOperationName : jobDefinition.operation,
                SYS_EXT_invokeMessage   : jobDefinition.message,
                SYS_externalJobType     : 'SOA'
        ]
    }
}
