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

    private WLSTInterpreter interpreter

    Configuration getConfiguration() {
        Threads.classLoader.addURL(this.project.artifact.file.toURL())
        def klass = Class.forName(this.configurationClass)
        klass.newInstance()
    }

    void execute() throws MojoExecutionException, MojoFailureException {
        def props = [
                (WLSTInterpreter.ENABLE_SCRIPT_MODE): Boolean.TRUE
        ]
        this.interpreter = new WLSTInterpreter(props)
        interpreter.exec("connect(url='${this.adminServerURL}', username='${this.weblogicUser}', password='${this.weblogicPassword}')")
        def config = this.configuration
        interpreter.exec('domainRuntime()')
        def jobDefDeployer = new JobDefDeployer(this.interpreter, config.hostingApplication)
        def existingDefs = jobDefDeployer.existingDefinitions
        this.log.info "Existing job definitions in app ${config.hostingApplication} are: ${existingDefs}"
        jobDefDeployer.updateDefinition(new JobDefinition(JobDefinition.Types.SyncWebserviceJobType,
                                                          'the new desc',
                                                          'http://localhost:8001/wsdl/path'.toURL(),
                                                          'service',
                                                          'port',
                                                          'operation',
                                                          '<message/>',
                                                          'test'))
        interpreter.exec('disconnect()')
    }
}
