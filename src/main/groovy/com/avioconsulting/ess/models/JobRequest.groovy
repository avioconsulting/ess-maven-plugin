package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class JobRequest extends BaseModel {
    JobDefinition jobDefinition
    RecurringSchedule schedule
    String description
}
