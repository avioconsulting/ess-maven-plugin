package com.avioconsulting.ess

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.glassfish.grizzly.threadpool.Threads
import org.reflections.Reflections

@Mojo(name = 'deploy')
class DeployMojo extends AbstractMojo {
    @Parameter(property = 'weblogic.user', required = true)
    private String weblogicUser

    @Parameter(property = 'weblogic.password', required = true)
    private String weblogicPassword

    @Parameter(property = 'admin.t3.url', required = true)
    private String adminServerURL

    @Parameter(property = 'soa.deploy.url', required = true)
    private String soaUrl

    @Parameter(property = 'ess.config.package', required = true)
    private String configurationPackage

    @Parameter(property = 'ess.host.app', defaultValue = 'EssNativeHostingApp')
    private String essHostingApp

    @Component
    private MavenProject project

    void execute() throws MojoExecutionException, MojoFailureException {
        def caller = new PythonCaller()
        caller.methodCall('connect', [
                url     : this.adminServerURL,
                username: this.weblogicUser,
                password: this.weblogicPassword
        ])
        // artifacts from our project, which is where the configuration is, won't be in the classpath by default
        Threads.classLoader.addURL(this.project.artifact.file.toURL())
        caller.methodCall('domainRuntime')
        def jobDefDeployer = new JobDefDeployer(caller, this.essHostingApp, this.soaUrl.toURL())
        def existingDefs = jobDefDeployer.existingDefinitions
        new Reflections(this.configurationPackage).getSubTypesOf(JobDefinition).each { klass ->
            def newJobDef = klass.newInstance()
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
