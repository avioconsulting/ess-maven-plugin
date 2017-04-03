package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.factories.JobRequestFactory
import com.avioconsulting.ess.factories.ScheduleFactory
import com.avioconsulting.ess.models.*
import com.avioconsulting.ess.wrappers.MetadataServiceWrapper
import com.avioconsulting.ess.wrappers.RuntimeServiceWrapper
import oracle.as.scheduler.MetadataService
import oracle.as.scheduler.MetadataServiceHandle
import oracle.as.scheduler.RuntimeService
import oracle.as.scheduler.RuntimeServiceHandle
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.joda.time.DateTimeZone
import weblogic.jndi.WLInitialContextFactory

import javax.naming.Context
import javax.naming.InitialContext

@SuppressWarnings("GroovyUnusedDeclaration")
@Mojo(name = 'jobSchedule')
class JobScheduleMojo extends CommonMojo {
    private final int DELETE_RETRIES = 10

    @Parameter(property = 'soa.deploy.url', required = true)
    private String soaDeployUrl

    // java:comp/env/ess/metadata, the jndiutil context, isn't present as a JNDI name on the EJB
    // so using the long name
    // JndiUtil.getMetadataServiceEJB(context)
    @Parameter(property = 'ess.metadata.ejb.jndiName', defaultValue = FieldConstants.ESS_JNDI_EJB_METADATA)
    private String essMetadataEjbJndi

    @Parameter(property = 'ess.runtime.ejb.jndiName', defaultValue = FieldConstants.ESS_JNDI_EJB_RUNTIME)
    private String essRuntimeEjbJndi

    @Parameter(property = 'ess.server.timezone', required = true)
    private String serverTimeZone

    @Parameter(property = 'ess.clean.everything.first', defaultValue = 'false')
    private boolean cleanFirst

    @Parameter(property = 'ess.hold.requests', defaultValue = 'false')
    private boolean holdRequests

    private InitialContext context

    private List<JobDefinition> newJobDefs = []
    private List<JobDefinition> updateJobDefs = []
    private List<JobDefinition> canceledJobDefs = []
    private List<RecurringSchedule> newSchedules = []
    private List<RecurringSchedule> updatedSchedules = []
    private List<JobRequest> newJobRequests = []
    private List<JobRequest> updatedJobRequests = []

    void execute() throws MojoExecutionException, MojoFailureException {
        withContext {
            if (this.cleanFirst) {
                cleanEverything()
            }

            withDeployerTransaction { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
                def existingDefs = metadataWrapper.existingDefinitions
                getSubTypesOf(JobDefinitionFactory).each { Class klass ->
                    def jobDefFactory = klass.newInstance() as JobDefinitionFactory
                    def jobDef = jobDefFactory.createJobDefinition()
                    def existingJob = existingDefs.contains(jobDef.name) ? metadataWrapper.getJobDefinition(
                            jobDef.name) : null
                    if (existingJob && existingJob.jobType != jobDef.jobType) {
                        this.log.info "Job definition ${jobDef.name} type has changed, therefore a new definition/requests must be created since ESS cannot update a job type..."
                        runtimeWrapper.cancelRequestsFor(jobDef)
                        canceledJobDefs << jobDef
                    } else if (existingJob) {
                        if (existingJob != jobDef) {
                            updateJobDefs << jobDef
                        } else {
                            this.log.info "Job definition ${jobDef.name} has not changed, skipping update..."
                        }
                    } else {
                        newJobDefs << jobDef
                    }
                }
            }

            deleteRequestsAndDefinitions(canceledJobDefs)
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
                getSubTypesOf(ScheduleFactory).each { Class klass ->
                    def scheduleFactory = klass.newInstance() as ScheduleFactory
                    def schedule = scheduleFactory.createSchedule()
                    logScheduleInfo(schedule)
                    if (existingSchedules.contains(schedule.name)) {
                        if (metadataWrapper.existingScheduleMatches(schedule)) {
                            this.log.info "Schedule ${schedule.name} has not changed, skipping update..."
                        } else {
                            this.updatedSchedules << schedule
                            this.log.info 'Updating schedule...'
                            metadataWrapper.updateSchedule(schedule)
                        }
                    } else {
                        this.newSchedules << schedule
                        this.log.info 'Creating schedule...'
                        metadataWrapper.createSchedule(schedule)
                    }
                }
                this.log.info 'Schedules complete'
            }

            // job requests are dependent on schedules+jobs being committed first
            withDeployerTransaction { MetadataServiceWrapper metadataWrapper, RuntimeServiceWrapper runtimeWrapper ->
                def existing = runtimeWrapper.existingJobRequests
                getSubTypesOf(JobRequestFactory).each { Class klass ->
                    def jobRequestFactory = klass.newInstance() as JobRequestFactory
                    def jobRequest = jobRequestFactory.createJobRequest()
                    def existingJobRequest = existing.find { data ->
                        data.scheduleName == jobRequest.schedule.name && data.jobDefinitionName == jobRequest.jobDefinition.name
                    }
                    if (existingJobRequest) {
                        if (!updatedSchedules.contains(jobRequest.schedule) && !updateJobDefs.contains(jobRequest.jobDefinition)) {
                            this.log.info "Job request '${jobRequest.submissionNotes}' will not be updated since schedule/job definition has not changed..."
                        } else {
                            this.updatedJobRequests << jobRequest
                            this.log.info "Updating job request ${jobRequest.submissionNotes}..."
                            runtimeWrapper.updateRequest(existingJobRequest)
                        }
                    } else {
                        this.newJobRequests << jobRequest
                        this.log.info "Creating job request ${jobRequest.submissionNotes}..."
                        runtimeWrapper.createRequest(jobRequest)
                    }
                }
                this.log.info 'Job requests complete'
            }
        }
    }

    private void deleteRequestsAndDefinitions(List<JobDefinition> canceledJobDefs) {
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
            case EveryMinuteSchedule:
                log.info "--- Interval     : ${schedule.repeatInterval}"
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
                (Context.INITIAL_CONTEXT_FACTORY): WLInitialContextFactory.name,
                (Context.PROVIDER_URL)           : this.soaWeblogicUrl,
                (Context.SECURITY_PRINCIPAL)     : this.weblogicUser,
                (Context.SECURITY_CREDENTIALS)   : this.weblogicPassword
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
        MetadataService svc = context.lookup(this.essMetadataEjbJndi) as MetadataService
        MetadataServiceHandle handle = svc.open()
        try {
            closure(svc, handle)
        }
        finally {
            try {
                svc.close(handle)
            }
            catch (e) {
                this.log.error('Unable to cleanup MetadataServiceHandle!', e)
            }
        }
    }

    private withRuntimeService(InitialContext context,
                               Closure closure) {
        RuntimeService svc = context.lookup(this.essRuntimeEjbJndi) as RuntimeService
        RuntimeServiceHandle handle = svc.open()
        try {
            closure(svc, handle)
        }
        finally {
            try {
                svc.close(handle)
            }
            catch (e) {
                this.log.error('Unable to cleanup RuntimeServiceHandle!', e)
            }
        }
    }

    private withDeployerTransaction(Closure closure) {
        withMetadataService(this.context) { MetadataService service, MetadataServiceHandle handle ->
            def logger = this.wrapperLogger
            def metadataWrapper = new MetadataServiceWrapper(service,
                                                             handle,
                                                             this.essHostingApp,
                                                             this.soaDeployUrl.toURL(),
                                                             DateTimeZone.forID(this.serverTimeZone),
                                                             logger,
                                                             this.essDeployPackage)
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
