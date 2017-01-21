package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.deployment.ESSDeployer
import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.factories.ScheduleFactory
import oracle.as.scheduler.MetadataService
import oracle.as.scheduler.MetadataServiceHandle
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.reflections.Reflections

import javax.naming.InitialContext

@Mojo(name = 'deploy')
class DeployMojo extends AbstractMojo {
    @Parameter(property = 'weblogic.user', required = true)
    private String weblogicUser

    @Parameter(property = 'weblogic.password', required = true)
    private String weblogicPassword

    @Parameter(property = 'soa.t3.url', required = true)
    private String soaWeblogicUrl

    @Parameter(property = 'soa.deploy.url', required = true)
    private String soaDeployUrl

    @Parameter(property = 'ess.config.package', required = true)
    private String configurationPackage

    @Parameter(property = 'ess.host.app', defaultValue = 'EssNativeHostingApp')
    private String essHostingApp

    // java:comp/env/ess/metadata, the jndiutil context, isnt present as a JNDI name on the EJB
    // so using the long name
    // JndiUtil.getMetadataServiceEJB(context)
    @Parameter(property = 'ess.metadata.ejb.jndiName', defaultValue = 'java:global.EssNativeHostingApp.native-ess-ejb.MetadataServiceBean!oracle.as.scheduler.MetadataServiceRemote')
    private String essMetadataEjbJndi

    @Component
    private MavenProject project

    void execute() throws MojoExecutionException, MojoFailureException {
        // artifacts from our project, which is where the configuration is, won't be in the classpath by default
        Thread.currentThread().contextClassLoader.addURL(this.project.artifact.file.toURL())
        withESSDeployer { ESSDeployer deployer ->
            def existingDefs = deployer.existingDefinitions
            def reflections = new Reflections(this.configurationPackage)
            reflections.getSubTypesOf(JobDefinitionFactory).each { klass ->
                def jobDefFactory = klass.newInstance()
                def jobDef = jobDefFactory.createJobDefinition()
                if (existingDefs.contains(jobDef.name)) {
                    this.log.info "Updating job definition ${jobDef.name}..."
                    deployer.updateDefinition(jobDef)
                } else {
                    this.log.info "Creating job definition ${jobDef.name}..."
                    deployer.createDefinition(jobDef)
                }
            }
            def existingSchedules = deployer.existingSchedules
            reflections.getSubTypesOf(ScheduleFactory).each { klass ->
                def scheduleFactory = klass.newInstance()
                def schedule = scheduleFactory.createSchedule()
                this.log.info "Schedule details for: ${schedule.name}"
                this.log.info "--- Display name: ${schedule.displayName}"
                this.log.info "--- Time of day: ${schedule.timeOfDay}"
                this.log.info "--- Days of week: ${schedule.daysOfWeek}"
                this.log.info "--- Start date: ${schedule.startDate}"
                this.log.info "--- End date: ${schedule.endDate}"
                this.log.info "--- Exclude dates: ${schedule.excludeDates}"
                this.log.info "--- Include dates: ${schedule.includeDates}"
                if (existingSchedules.contains(schedule.name)) {
                    // update
                    this.log.info 'Updating...'
                    deployer.updateSchedule(schedule)
                }
                else {
                    this.log.info 'Creating...'
                    deployer.createSchedule(schedule)
                }
            }
        }
    }

    private withContext(Closure closure) {
        Hashtable<String, String> props = [
                'java.naming.factory.initial'     : 'weblogic.jndi.WLInitialContextFactory',
                'java.naming.provider.url'        : this.soaWeblogicUrl,
                'java.naming.security.principal'  : this.weblogicUser,
                'java.naming.security.credentials': this.weblogicPassword
        ]
        def context = new InitialContext(props)
        try {
            closure(context)
        }
        finally {
            context.close()
        }
    }

    private withMetadataService(InitialContext context,
                                Closure closure) {
        MetadataService svc = context.lookup(this.essMetadataEjbJndi)
        MetadataServiceHandle handle = svc.open()
        try {
            closure(svc, handle)
        }
        finally {
            svc.close(handle)
        }
    }

    private withESSDeployer(Closure closure) {
        withContext { InitialContext context ->
            withMetadataService(context) { MetadataService service, MetadataServiceHandle handle ->
                closure(new ESSDeployer(service, handle, this.essHostingApp, this.soaDeployUrl.toURL()))
            }
        }
    }
}
