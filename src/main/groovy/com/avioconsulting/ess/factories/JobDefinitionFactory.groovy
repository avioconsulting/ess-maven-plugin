package com.avioconsulting.ess.factories

import com.avioconsulting.ess.models.JobDefinition

interface JobDefinitionFactory {
    JobDefinition createJobDefinition()
}