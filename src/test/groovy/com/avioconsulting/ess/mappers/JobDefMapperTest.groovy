package com.avioconsulting.ess.mappers

import com.avioconsulting.ess.models.JobDefinition
import oracle.as.scheduler.MetadataObjectId
import oracle.as.scheduler.ParameterList
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class JobDefMapperTest {
    def Map getMap(ParameterList params) {
        params.getAll().collectEntries { param ->
            [param.name, param.value]
        }
    }

    @Test
    void getOracleJobDef() {
        // arrange
        def input = new JobDefinition(jobType: JobDefinition.Types.SyncWebserviceJobType,
                                      description: 'the desc',
                                      wsdlPath: '/wsdl/path',
                                      service: 'the_service',
                                      port: 'the_port',
                                      operation: 'the_operation',
                                      message: '<message/>',
                                      name: 'the name')

        // act
        def result = JobDefMapper.getOracleJobDef('http://www.foo.com'.toURL(), input)

        // assert
        assertThat result.name,
                   is(equalTo('the name'))
        assertThat result.description,
                   is(equalTo('the desc'))
        def metadataObjectId = result.jobType
        assertThat metadataObjectId.namePart,
                   is(equalTo('the name'))
        assertThat metadataObjectId.type,
                   is(equalTo(MetadataObjectId.MetadataObjectType.JOB_DEFINITION))
        assertThat metadataObjectId.packagePart,
                   is(equalTo('/oracle/as'))
        def params = getMap(result.parameters)
        assertThat params,
                   is(equalTo([
                           SYS_effectiveApplication: 'EssNativeHostingApp',
                           SYS_EXT_wsWsdlBaseUrl   : 'http://www.foo.com',
                           SYS_EXT_wsWsdlUrl       : '/wsdl/path',
                           SYS_EXT_wsServiceName   : 'the_service',
                           SYS_EXT_wsPortName      : 'the_port',
                           SYS_EXT_wsOperationName : 'the_operation',
                           SYS_EXT_invokeMessage   : '<message/>',
                           SYS_externalJobType     : 'SOA'
                   ]))
    }
}
