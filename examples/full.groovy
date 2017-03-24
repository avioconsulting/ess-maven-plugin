package foobar

import com.avioconsulting.ess.models.*
import com.avioconsulting.ess.factories.*
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import com.avioconsulting.ess.builders.*
import java.time.DayOfWeek

class SimplyBetter implements JobDefinitionFactory, ScheduleFactory, JobRequestFactory {
  JobDefinition createJobDefinition() {
    new JobDefinition(jobType: JobDefinition.Types.SyncWebService,
                      description: 'the description4',
                      wsdlPath: '/some/wsdl6',
                      service: 'the_service_3',
                      port: 'the_port',
                      operation: 'the_operation',
                      message: '<message44/>',
                      name: 'SimplyBetter')
  }

  RecurringSchedule createSchedule() {
    def holidays = [
      '2017-01-02',
      '2017-02-20',
      '2017-05-29',
      '2017-07-04',
      '2017-09-04',
      '2017-11-23',
      '2017-11-24',
      '2017-12-25'
    ].collect { dateStr -> new LocalDate(dateStr) }

    ScheduleBuilder.getWeeklySchedule name: 'SimplyBetter',
                                      displayName: 'the schedule',
                                      description: 'Weekly schedule on mondays',
                                      startDate: new LocalDate(2017, 1, 1),
                                      endDate: new LocalDate(2017, 12, 31),
                                      timeOfDay: new LocalTime(9, 15, 10),
                                      timeZone: DateTimeZone.forID('America/Chicago'),
                                      daysOfWeek: [DayOfWeek.MONDAY],
                                      holidays: holidays,
                                      alternateDirection: Direction.Backward
  }

  JobRequest createJobRequest() {
    // remember the purpose of the job request is to tie together the schedule and job definition
    // the reason for calling createSchedule() and createJobDefinition() is to make this dependency
    // explicit and provide an easy way for the job request to be created and point to the name
    // of the schedule/job definition
    new JobRequest(description: 'recurring job',
                   schedule: createSchedule(),
                   jobDefinition: createJobDefinition())
  }
}
