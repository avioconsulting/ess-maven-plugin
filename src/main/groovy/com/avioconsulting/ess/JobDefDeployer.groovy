package com.avioconsulting.ess

import org.python.core.PyDictionary
import org.python.core.PyFunction
import org.python.core.PyInteger
import org.python.core.PyObject
import org.python.core.PyString
import weblogic.management.scripting.utils.WLSTInterpreter

import java.util.regex.Pattern

class JobDefDeployer {
    private final WLSTInterpreter interpreter
    private final String hostingApplication

    JobDefDeployer(WLSTInterpreter interpreter, String hostingApplication) {
        this.hostingApplication = hostingApplication
        this.interpreter = interpreter
    }

    List<String> getExistingDefinitions() {
        // command outputs to stdout and doesn't return anything
        def writer = new StringWriter()
        def interpreter = this.interpreter
        def origOut = interpreter.out
        interpreter.out = writer
        try {
            interpreter.exec("manageSchedulerJobDefn('SHOW', '${this.hostingApplication}')")
        }
        finally {
            interpreter.out = origOut
        }
        writer.close()
        return parseDefinitions(writer.toString())
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
        PyFunction func = interpreter.get 'manageSchedulerJobDefn'
        assert func != null
        PyObject[] args = [
                new PyString(operation),
                new PyString(this.hostingApplication),
                new PyString(jobDefinition.name),
                new PyString(jobDefinition.jobType.toString()),
                new PyString(jobDefinition.description),
                getStringDictionary(getProperties(jobDefinition))
        ].toArray()
        String[] kwArgs = ['jobName', 'jobType', 'desc', 'props'].toArray()
        func.__call__(args, kwArgs)
        //interpreter.exec("manageSchedulerJobDefn('${operation}', '${this.hostingApplication}', jobName=)")
    }

    static PyDictionary getStringDictionary(map) {
        def dict = new PyDictionary()
        map.each { key, value ->
            PyObject wrapped
            switch (value) {
                case String:
                    wrapped = new PyString(value)
                    break
                case Integer:
                    wrapped = new PyInteger(value)
                    break
                case PyDictionary:
                    wrapped = value
                    break
                default:
                    throw new Exception("Unknown value/type ${value}, ${value..getClass()}!")
            }
            dict.__setitem__(new PyString(key), wrapped)
        }
        dict
    }

    // TODO: Dlete it
    static String getPythonDict(map) {
        def values = map.collect { key, value ->
            "'${key}': '${value}'"
        }.join(', ')
        "{${values}}"
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
