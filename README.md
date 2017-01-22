# ESS Maven Plugin

Functionality:
* Takes care of EJB interactions with ESS
* Easy to write Groovy classes are used in your project POMs to declare schedules (when), job definitions (what), and job requests (ties together schedule + job definition)
* Updates existing job definitions/schedules in place
* Updates job requests when the schedule is updated

## Usage

### POM Setup
 
In your project POM, it's important to set 2 properties:
1. The `ess.server.timezone` POM property (or `serverTimeZone` plugin/config) needs to match the time zone of the server you deploy to. This is due to observed quirks with deploying to ESS
2. The `ess.config.package` POM property (or `configurationPackage` plugin/config) should be set to the Java/Groovy package that your expose your factories (see below)

### Factories

Create Groovy (or Java) classes in your project that implement the `JobDefinitionFactory`, `ScheduleFactory`, `JobRequestFactory` interfaces from the `com.avioconsulting.ess.factories package` in this plugin. The plugin automatically will find them and use them if you set `ess.config.package` properly in your POM.

Example:

```groovy
package foobar

import com.avioconsulting.ess.models.*
import com.avioconsulting.ess.factories.*
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import com.avioconsulting.ess.builders.ScheduleBuilder

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

    ScheduleBuilder.getSchedule name: 'SimplyBetter',
                                displayName: 'the schedule',
                                description: 'Weekly schedule on mondays',
                                startDate: new LocalDate(2017, 1, 1),
                                endDate: new LocalDate(2017, 12, 31),
                                timeOfDay: new LocalTime(9, 15, 10),
                                timeZone: DateTimeZone.forID('America/Denver'),
                                daysOfWeek: [RecurringSchedule.DayOfWeek.Monday],
                                holidays: holidays,
                                alternateDirection: Direction.Backward
  }

  JobRequest createJobRequest() {
    new JobRequest(description: 'recurring job',
                   schedule: createSchedule(),
                   jobDefinition: createJobDefinition())
  }
}
```

## FAQ

### What happens if schedules, etc. are already out there?

The schedule and job definition will be updated in place. If the job request already exists, it will be updated to ensure it matches the schedule. 

## How will this look in EM?

The way ESS seems to work is that there are 2 visible job requests for each "setup" you make. The parent one, which lasts the life of the schedule and then a child one that represents the next execution. Whenever you run this deployment with an existing job request, the next pending execution will be canceled and a new one will be created with the proper date for the updated schedule. The original parent will remain.

### Are deletions supported?

Not right now.

### Purging

You can force deletion of ALL ESS data by running with the property `ess.clean.everything.first` on the Maven command line.