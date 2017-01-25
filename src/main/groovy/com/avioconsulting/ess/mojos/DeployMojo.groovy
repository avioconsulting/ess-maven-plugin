package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.factories.JobRequestFactory
import com.avioconsulting.ess.factories.ScheduleFactory
import com.avioconsulting.ess.models.JobDefinition
import com.avioconsulting.ess.models.MonthlySchedule
import com.avioconsulting.ess.models.RecurringSchedule
import com.avioconsulting.ess.models.WeeklySchedule
import com.avioconsulting.ess.wrappers.MetadataServiceWrapper
import com.avioconsulting.ess.wrappers.RuntimeServiceWrapper
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
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

import javax.naming.InitialContext

@Mojo(name = 'deploy')
class DeployMojo extends AbstractMojo {
    private final int DELETE_RETRIES = 10

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

    @Parameter(property = 'ess.clean.everything.first', defaultValue = 'false')
    private boolean cleanFirst

    @Parameter(property = 'ess.hold.requests', defaultValue = 'false')
    private boolean holdRequests

    @Component
    private MavenProject project

    private InitialContext context

    void execute() throws MojoExecutionException, MojoFailureException {
        withContext {
            if (this.cleanFirst) {
                cleanEverything()
            }

            def reflections = getReflectionsUtility()
            List<JobDefinition> newJobDefs = []
            List<JobDefinition> updateJobDefs = []
            List<JobDefinition> canceledJobDefs = []
            withDeployerTransaction { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
                def existingDefs = metadataWrapper.existingDefinitions
                reflections.getSubTypesOf(JobDefinitionFactory).each { klass ->
                    def jobDefFactory = klass.newInstance()
                    def jobDef = jobDefFactory.createJobDefinition()
                    def existingJob = existingDefs.contains(jobDef.name)
                    if (existingJob && metadataWrapper.hasJobDefinitionTypeChanged(jobDef)) {
                        this.log.info "Job definition ${jobDef.name} type has changed, therefore a new definition/requests must be created since ESS cannot update a job type..."
                        runtimeWrapper.cancelRequestsFor(jobDef)
                        canceledJobDefs << jobDef
                    } else if (existingJob) {
                        updateJobDefs << jobDef
                    } else {
                        newJobDefs << jobDef
                    }
                }
            }

            cancelAndDeleteRequests(canceledJobDefs)
            newJobDefs.addAll(canceledJobDefs)

            withDeployerTransaction { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
                newJobDefs.each { jobDef ->
                    this.log.info "Creating job definition ${jobDef.name}..."
                    metadataWrapper.createDefinition(jobDef)
                }
                updateJobDefs.each { jobDef ->
                    this.log.info "Updating job definition ${jobDef.name}..."
                    metadataWrapper.updateDefinition(jobDef)
                }

                this.log.info 'Job definitions complete'

                def existingSchedules = metadataWrapper.existingSchedules
                reflections.getSubTypesOf(ScheduleFactory).each { klass ->
                    def scheduleFactory = klass.newInstance()
                    def schedule = scheduleFactory.createSchedule()
                    logScheduleInfo(schedule)
                    if (existingSchedules.contains(schedule.name)) {
                        // update
                        this.log.info 'Updating schedule...'
                        metadataWrapper.updateSchedule(schedule)
                    } else {
                        this.log.info 'Creating schedule...'
                        metadataWrapper.createSchedule(schedule)
                    }
                }
            }

            // job requests are dependent on schedules+jobs being committed first
            withDeployerTransaction { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
                def existing = runtimeWrapper.existingJobRequests
                reflections.getSubTypesOf(JobRequestFactory).each { klass ->
                    def jobRequestFactory = klass.newInstance()
                    def jobRequest = jobRequestFactory.createJobRequest()
                    def existingJobRequest = existing.find { data ->
                        data.scheduleName == jobRequest.schedule.name && data.jobDefinitionName == jobRequest.jobDefinition.name
                    }
                    if (existingJobRequest) {
                        this.log.info "Updating job request ${jobRequest.submissionNotes}..."
                        runtimeWrapper.updateRequest(existingJobRequest)
                    } else {
                        this.log.info "Creating job request ${jobRequest.submissionNotes}..."
                        runtimeWrapper.createRequest(jobRequest)
                    }
                }
            }
        }
    }

    private void cancelAndDeleteRequests(List<JobDefinition> canceledJobDefs) {
        // need 2 transactions for cancel/delete job type changes
        deployerWithRetries { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
            canceledJobDefs.each { jobDef ->
                runtimeWrapper.deleteRequestsFor(jobDef)
            }
        }

        withDeployerTransaction { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
            canceledJobDefs.each { jobDef ->
                metadataWrapper.deleteDefinition(jobDef)
            }
        }
    }

    private void logScheduleInfo(RecurringSchedule schedule) {
        def log = this.log
        log.info "Schedule details for: ${schedule.name}"
        log.info "--- Display name : ${schedule.displayName}"
        log.info "--- Time of day  : ${schedule.timeOfDay}"
        log.info "--- Frequency    : ${schedule.frequency}"
        switch (schedule) {
            case WeeklySchedule:
                log.info "--- Days of week : ${schedule.daysOfWeek}"
                break
            case MonthlySchedule:
                log.info "--- Days of month: ${schedule.daysOfMonth}"
                break
            default:
                throw new Exception(
                        "Unknown schedule type ${schedule.getClass()}! A developer did not do their job!")
        }
        log.info "--- Start date   : ${schedule.startDate}"
        log.info "--- End date     : ${schedule.endDate}"
        log.info "--- Exclude dates: ${schedule.excludeDates}"
        log.info "--- Include dates: ${schedule.includeDates}"
    }

    private getReflectionsUtility() {
        // artifacts from our project, which is where the configuration is, won't be in the classpath by default
        ClasspathHelper.contextClassLoader().addURL(this.project.artifact.file.toURL())
        // used more complex config/construction due to
        def configBuilder = new ConfigurationBuilder()
                .addClassLoader(ClasspathHelper.contextClassLoader())
                .addClassLoader(ClasspathHelper.staticClassLoader())
                .addUrls(this.project.artifact.file.toURL())
                .forPackages(this.configurationPackage)

        new Reflections(configBuilder)
    }

    def cleanEverything() {
        // put this in 2 different transactions so that cancel takes effect
        withDeployerTransaction { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
            runtimeWrapper.cancelAllRequests()
        }
        deployerWithRetries { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
            runtimeWrapper.deleteAllRequests()
            metadataWrapper.deleteAllSchedules()
            metadataWrapper.deleteAllDefinitions()
        }
    }

    def deployerWithRetries(Closure closure) {
        [1..DELETE_RETRIES][0].find { index ->
            try {
                // when we retry, we have to start a whole new transaction
                withDeployerTransaction {
                    MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
                        closure(metadataWrapper, runtimeWrapper)
                }
                return true
            }
            catch (e) {
                if (index == DELETE_RETRIES) {
                    throw new Exception("Tried ${index} times and failed!", e)
                }
                this.log.info "Delete requests try ${index} failed, sleeping 2 sec"
                Thread.sleep(2000)
                return false
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
        this.context = new InitialContext(props)
        try {
            closure()
        }
        finally {
            this.context.close()
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

    private withDeployerTransaction(Closure closure) {
        withMetadataService(this.context) { MetadataService service, MetadataServiceHandle handle ->
            def logger = { String msg -> this.log.info msg }
            def metadataWrapper = new MetadataServiceWrapper(service,
                                                             handle,
                                                             this.essHostingApp,
                                                             this.soaDeployUrl.toURL(),
                                                             DateTimeZone.forID(this.serverTimeZone),
                                                             logger)
            withRuntimeService(this.context) { RuntimeService runSvc, RuntimeServiceHandle runHandle ->
                def runtimeWrapper = new RuntimeServiceWrapper(runSvc,
                                                               runHandle,
                                                               metadataWrapper,
                                                               logger,
                                                               this.holdRequests)
                closure(metadataWrapper, runtimeWrapper)
            }
        }
    }
}
