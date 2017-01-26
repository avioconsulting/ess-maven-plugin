package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.factories.EssClientPolicySubjectFactory
import com.avioconsulting.util.PythonCaller
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = 'attachPolicies')
class WsmMojo extends CommonMojo {
    @Parameter(property = 'admin.t3.url', required = true)
    private String adminServerURL

    void execute() throws MojoExecutionException, MojoFailureException {
        def caller = new PythonCaller()
        caller.methodCall('connect', [
                url     : this.adminServerURL,
                username: this.weblogicUser,
                password: this.weblogicPassword
        ])
        def reflections = getReflectionsUtility()
        reflections.getSubTypesOf(EssClientPolicySubjectFactory).each { klass ->
            def policy = klass.newInstance().createPolicySubject()
            println "got policy ${policy}"
        }
        caller.methodCall('beginWSMSession')
        caller.methodCall('commitWSMSession')
        caller.methodCall('disconnect')
    }
}
