package com.avioconsulting.ess

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.glassfish.grizzly.threadpool.Threads
import weblogic.management.scripting.utils.WLSTInterpreter

@Mojo(name='deploy')
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
        Threads.classLoader.addURL(this.project.artifact.file.toURL())
        def klass = Class.forName(this.configurationClass)
        klass.newInstance()
    }

    void execute() throws MojoExecutionException, MojoFailureException {
        def props = [
                (WLSTInterpreter.ENABLE_SCRIPT_MODE): Boolean.TRUE
        ]
        def interpreter = new WLSTInterpreter(props)
        interpreter.exec("connect(url='${this.adminServerURL}', username='${this.weblogicUser}', password='${this.weblogicPassword}')")
        def config = this.configuration
        interpreter.exec('domainRuntime()')
        interpreter.exec("manageSchedulerJobDefn('SHOW', '${config.hostingApplication}')")
        interpreter.exec('disconnect()')
    }
}
