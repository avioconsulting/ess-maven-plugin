package com.avioconsulting.ess.deployment

import com.avioconsulting.ess.mappers.JobDefMapper
import com.avioconsulting.ess.mappers.ScheduleMapper
import com.avioconsulting.ess.models.JobDefinition
import com.avioconsulting.ess.models.RecurringSchedule
import oracle.as.scheduler.Filter
import oracle.as.scheduler.MetadataObjectId
import oracle.as.scheduler.MetadataService
import oracle.as.scheduler.MetadataServiceHandle
import org.joda.time.DateTimeZone

class ESSDeployer {
    private final String hostingApplication
    private final URL soaUrl
    private final MetadataService service
    private final MetadataServiceHandle handle
    private static final String PACKAGE_NAME_WHEN_CREATED_VIA_EM = '/oracle/apps/ess/custom/'
    private static final Filter everythingFilter = new Filter('name',
                                                              Filter.Comparator.NOT_EQUALS,
                                                              '')
    private final ScheduleMapper scheduleMapper

    ESSDeployer(MetadataService service,
                MetadataServiceHandle handle,
                String hostingApplication,
                URL soaUrl,
                DateTimeZone serverTimeZone) {
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
        def id = MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_DEFINITION,
                                                         PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                         definition.name)
        this.service.updateJobDefinition(this.handle,
                                         id,
                                         oracleDef)
    }

    def createSchedule(RecurringSchedule schedule) {
        def oracleSchedule = this.scheduleMapper.getOracleSchedule(schedule)
        this.service.addScheduleDefinition(this.handle,
                                           oracleSchedule,
                                           PACKAGE_NAME_WHEN_CREATED_VIA_EM)
    }

    def updateSchedule(RecurringSchedule schedule) {
        def oracleSchedule = this.scheduleMapper.getOracleSchedule(schedule)
        def id = MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.SCHEDULE_DEFINITION,
                                                         PACKAGE_NAME_WHEN_CREATED_VIA_EM,
                                                         schedule.name)
        this.service.updateScheduleDefinition(this.handle,
                                              id,
                                              oracleSchedule)
    }
}
