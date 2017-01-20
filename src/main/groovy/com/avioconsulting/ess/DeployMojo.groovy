package com.avioconsulting.ess

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.glassfish.grizzly.threadpool.Threads

@Mojo(name = 'deploy')
class DeployMojo extends AbstractMojo {
    @Parameter(property = 'weblogic.user', required = true)
    private String weblogicUser

    @Parameter(property = 'weblogic.password', required = true)
    private String weblogicPassword

    @Parameter(property = 'admin.t3.url', required = true)
    private String adminServerURL

    @Parameter(property = 'ess.config.class', required = true)
    private String configurationClass

    @Component
    private MavenProject project

    Configuration getConfiguration() {
        // artifacts from our project, which is where the configuration is, won't be in the classpath by default
        Threads.classLoader.addURL(this.project.artifact.file.toURL())
        def klass = Class.forName(this.configurationClass)
        klass.newInstance()
    }

    void execute() throws MojoExecutionException, MojoFailureException {
        def caller = new PythonCaller()
        caller.methodCall('connect', [
                url     : this.adminServerURL,
                username: this.weblogicUser,
                password: this.weblogicPassword
        ])
        def config = this.configuration
        caller.methodCall('domainRuntime')
        def jobDefDeployer = new JobDefDeployer(caller, config.hostingApplication)
        def existingDefs = jobDefDeployer.existingDefinitions
        configuration.jobDefinitions.each { newJobDef ->
            if (existingDefs.contains(newJobDef.name)) {
                jobDefDeployer.updateDefinition(newJobDef)
            }
            else {
                jobDefDeployer.createDefinition(newJobDef)
            }
        }
        caller.methodCall('disconnect')
    }
}
