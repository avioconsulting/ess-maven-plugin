package com.avioconsulting.ess.wrappers

import com.avioconsulting.ess.mappers.JobDefMapper
import com.avioconsulting.ess.mappers.ScheduleMapper
import com.avioconsulting.ess.models.JobDefinition
import com.avioconsulting.ess.models.RecurringSchedule
import oracle.as.scheduler.*
import org.joda.time.DateTimeZone

class MetadataServiceWrapper {
    private final String hostingApplication
    private final URL soaUrl
    private final MetadataService service
    private final MetadataServiceHandle handle
    public static final String PACKAGE_NAME_WHEN_CREATED_VIA_EM = '/oracle/apps/ess/custom/'
    // should result in everything being returned
    private static final Filter everythingFilter = null
    private final ScheduleMapper scheduleMapper
    private final Closure logger

    MetadataServiceWrapper(MetadataService service,
                           MetadataServiceHandle handle,
                           String hostingApplication,
                           URL soaUrl,
                           DateTimeZone serverTimeZone,
                           Closure logger) {
        this.logger = logger
        this.handle = handle
        this.service = service
        this.soaUrl = soaUrl
        this.hostingApplication = hostingApplication
        this.scheduleMapper = new ScheduleMapper(serverTimeZone)
    }

    List<String> getExistingDefinitions() {
        Enumeration<MetadataObjectId> result = this.service.queryJobDefinitions(this.handle,
                                                                                everythingFilter,
                                                                                MetadataService.QueryField.NAME,
                                                                                true)
        result.collect { id -> id.namePart }
    }

    List<String> getExistingSchedules() {
        Enumeration<MetadataObjectId> result = this.service.querySchedules(this.handle,
                                                                           everythingFilter,
                                                                           MetadataService.QueryField.NAME,
                                                                           true)
        result.collect { id -> id.namePart }
    }

    static MetadataObjectId getJobDefId(String name) {
        MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_DEFINITION,
                                                PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                name)
    }

    def deleteAllDefinitions() {
        def existing = getExistingDefinitions()
        existing.each { definition ->
            def id = getJobDefId definition
            this.logger "Deleting job definition ${id}"
            this.service.deleteJobDefinition(this.handle, id)
        }
    }

    def createDefinition(JobDefinition definition) {
        def oracleDef = JobDefMapper.getOracleJobDef(this.soaUrl,
                                                     this.hostingApplication,
                                                     definition)
        this.service.addJobDefinition(this.handle,
                                      oracleDef,
                                      PACKAGE_NAME_WHEN_CREATED_VIA_EM)
    }

    def updateDefinition(JobDefinition definition) {
        def oracleDef = JobDefMapper.getOracleJobDef(this.soaUrl,
                                                     this.hostingApplication,
                                                     definition)
        def id = getJobDefId definition.name
        this.service.updateJobDefinition(this.handle,
                                         id,
                                         oracleDef)
    }

    def deleteDefinition(JobDefinition definition) {
        def id = getJobDefId definition.name
        this.logger "Deleting job definition ${id}"
        this.service.deleteJobDefinition(this.handle, id)
    }

    boolean hasJobDefinitionTypeChanged(JobDefinition jobDefinition) {
        def oracle = getOracleJobDefinition(jobDefinition.name)
        def existingJobTypeOracle = oracle.jobType.namePart
        def existingJobTypeUs = JobDefMapper.reverseTypeMapping[existingJobTypeOracle]
        existingJobTypeUs != jobDefinition.jobType
    }

    oracle.as.scheduler.JobDefinition getOracleJobDefinition(String name) {
        def id = getJobDefId name
        return this.service.getJobDefinition(this.handle, id, false)
    }

    Schedule getOracleSchedule(RecurringSchedule schedule) {
        def id = getScheduleId schedule.name
        return this.service.getScheduleDefinition(this.handle, id, false)
    }

    static MetadataObjectId getScheduleId(String name) {
        MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.SCHEDULE_DEFINITION,
                                                PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                name)
    }

    def deleteAllSchedules() {
        def existing = getExistingSchedules()
        existing.each { schedule ->
            def id = getScheduleId schedule
            this.logger "Deleting schedule ${id}..."
            this.service.deleteScheduleDefinition(this.handle, id)
        }
    }

    def createSchedule(RecurringSchedule schedule) {
        def oracleSchedule = this.scheduleMapper.getOracleSchedule(schedule)
        this.service.addScheduleDefinition(this.handle,
                                           oracleSchedule,
                                           PACKAGE_NAME_WHEN_CREATED_VIA_EM)
    }

    def updateSchedule(RecurringSchedule schedule) {
        def oracleSchedule = this.scheduleMapper.getOracleSchedule(schedule)
        def id = getScheduleId schedule.name
        this.service.updateScheduleDefinition(this.handle,
                                              id,
                                              oracleSchedule)
    }
}
