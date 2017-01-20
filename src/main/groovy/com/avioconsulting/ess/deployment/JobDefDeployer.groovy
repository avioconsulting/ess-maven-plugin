package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.models.JobDefinition
import oracle.as.scheduler.Filter
import oracle.as.scheduler.MetadataService
import oracle.as.scheduler.MetadataServiceHandle

import java.util.regex.Pattern

class JobDefDeployer {
    private final String hostingApplication
    private final URL soaUrl
    private final MetadataService service
    private final MetadataServiceHandle handle

    JobDefDeployer(MetadataService service, MetadataServiceHandle handle, String hostingApplication, URL soaUrl) {
        this.handle = handle
        this.service = service
        this.soaUrl = soaUrl
        this.hostingApplication = hostingApplication
    }

    List<String> getExistingDefinitions() {
        def result = this.service.queryJobDefinitions(this.handle,
                                                      new Filter('name',
                                                                 Filter.Comparator.NOT_EQUALS,
                                                                 ''),
                                                      MetadataService.QueryField.NAME,
                                                      true)
        result.each { id ->
            println "got result ${id}... and package ${id.packagePart}"
        }
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
        def matcher = new Pattern(
                ".*Job Definitions present in namespace of \"${this.hostingApplication}\" are: \$(.*)",
                flags).matcher(output)
        assert matcher.matches()
        def listing = matcher.group(1)
        matcher = new Pattern(/(\S+): JobDefinition:\/\S+/, flags).matcher(listing)
        matcher.collect { m -> m[1] }
    }

    def createDefinition(JobDefinition definition) {
        def packageName = null
        this.service.addJobDefinition(this.handle, jobDev, packageName)
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
}
