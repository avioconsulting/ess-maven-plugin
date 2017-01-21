package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.deployment.MetadataWrapper
import com.avioconsulting.ess.deployment.RuntimeWrapper
import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.factories.ScheduleFactory
import com.avioconsulting.ess.models.JobDefinition
import com.avioconsulting.ess.models.JobRequest
import com.avioconsulting.ess.models.RecurringSchedule
import oracle.as.scheduler.MetadataService
import oracle.as.scheduler.MetadataServiceHandle
import oracle.as.scheduler.RuntimeService
import oracle.as.scheduler.RuntimeServiceHandle
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.joda.time.DateTimeZone
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
    @Parameter(property = 'ess.metadata.ejb.jndiName',
            defaultValue = 'java:global.EssNativeHostingApp.native-ess-ejb.MetadataServiceBean!oracle.as.scheduler.MetadataServiceRemote')
    private String essMetadataEjbJndi

    @Parameter(property = 'ess.runtime.ejb.jndiName',
            defaultValue = 'java:global.EssNativeHostingApp.native-ess-ejb.RuntimeServiceBean!oracle.as.scheduler.RuntimeServiceRemote')
    private String essRuntimeEjbJndi

    @Parameter(property = 'ess.server.timezone', required = true)
    private String serverTimeZone

    @Component
    private MavenProject project

    void execute() throws MojoExecutionException, MojoFailureException {
        // artifacts from our project, which is where the configuration is, won't be in the classpath by default
        Thread.currentThread().contextClassLoader.addURL(this.project.artifact.file.toURL())
        withDeployers { MetadataWrapper metadataWrapper, RuntimeWrapper runtimeWrapper ->
            def existingDefs = metadataWrapper.existingDefinitions
            def reflections = new Reflections(this.configurationPackage)
            JobDefinition jobDef
            reflections.getSubTypesOf(JobDefinitionFactory).each { klass ->
                def jobDefFactory = klass.newInstance()
                jobDef = jobDefFactory.createJobDefinition()
                if (existingDefs.contains(jobDef.name)) {
                    this.log.info "Updating job definition ${jobDef.name}..."
                    metadataWrapper.updateDefinition(jobDef)
                } else {
                    this.log.info "Creating job definition ${jobDef.name}..."
                    metadataWrapper.createDefinition(jobDef)
                }
            }
            def existingSchedules = metadataWrapper.existingSchedules
            RecurringSchedule schedule
            reflections.getSubTypesOf(ScheduleFactory).each { klass ->
                def scheduleFactory = klass.newInstance()
                schedule = scheduleFactory.createSchedule()
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
                    metadataWrapper.updateSchedule(schedule)
                } else {
                    this.log.info 'Creating...'
                    metadataWrapper.createSchedule(schedule)
                }
            }
            // TESTING
            runtimeWrapper.doesJobRequestExist(new JobRequest(jobDefinition: jobDef,
                                                              schedule: schedule,
                                                              description: 'the request'))
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

    private withRuntimeService(InitialContext context,
                               Closure closure) {
        RuntimeService svc = context.lookup(this.essRuntimeEjbJndi)
        RuntimeServiceHandle handle = svc.open()
        try {
            closure(svc, handle)
        }
        finally {
            svc.close(handle)
        }
    }

    private withDeployers(Closure closure) {
        withContext { InitialContext context ->
            withMetadataService(context) { MetadataService service, MetadataServiceHandle handle ->
                def metadataWrapper = new MetadataWrapper(service,
                                                          handle,
                                                          this.essHostingApp,
                                                          this.soaDeployUrl.toURL(),
                                                          DateTimeZone.forID(this.serverTimeZone))
                withRuntimeService(context) { RuntimeService runSvc, RuntimeServiceHandle runHandle ->
                    def runtimeWrapper = new RuntimeWrapper(runSvc, runHandle, metadataWrapper)
                    closure(metadataWrapper, runtimeWrapper)
                }
            }
        }
    }
}
