package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.models.JobDefinition
import com.avioconsulting.ess.util.PythonCaller

import java.util.regex.Pattern

class JobDefDeployer {
    private final String hostingApplication
    private final PythonCaller caller
    private final URL soaUrl

    JobDefDeployer(PythonCaller caller, String hostingApplication, URL soaUrl) {
        this.soaUrl = soaUrl
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
        // these property names come from the web service template in EM
        [
                SYS_effectiveApplication: this.hostingApplication,
                SYS_EXT_wsWsdlBaseUrl   : soaUrl.toString(),
                SYS_EXT_wsWsdlUrl       : jobDefinition.wsdlPath,
                SYS_EXT_wsServiceName   : jobDefinition.service,
                SYS_EXT_wsPortName      : jobDefinition.port,
                SYS_EXT_wsOperationName : jobDefinition.operation,
                SYS_EXT_invokeMessage   : jobDefinition.message,
                SYS_externalJobType     : 'SOA'
        ]
    }
}
