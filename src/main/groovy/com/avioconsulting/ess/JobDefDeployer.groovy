package com.avioconsulting.ess

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

    }

    def updateDefinition(JobDefinition definition) {

    }
}
