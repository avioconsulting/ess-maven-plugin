package com.avioconsulting.ess

import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.models.JobDefinition

class IntegrationFactory implements JobDefinitionFactory {
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
}
