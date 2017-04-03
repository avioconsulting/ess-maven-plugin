package com.avioconsulting.ess

import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.models.JobDefinition

class UpdatedJobDefFactory implements JobDefinitionFactory {
    JobDefinition createJobDefinition() {
        new JobDefinition(jobType: JobDefinition.Types.SyncWebService,
                          description: 'the description45',
                          wsdlPath: '/some/wsdl7',
                          service: 'the_service_4',
                          port: 'the_port4',
                          operation: 'the_operation4',
                          message: '<message45/>',
                          name: 'FirstJobDef')
    }
}
