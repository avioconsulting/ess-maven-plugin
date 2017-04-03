package com.avioconsulting.ess.mappers

import com.avioconsulting.ess.models.JobDefinition
import oracle.as.scheduler.MetadataObjectId
import oracle.as.scheduler.ParameterList
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

class JobDefMapperTest {
    Map getMap(ParameterList params) {
        params.getAll().collectEntries { param ->
            [param.name, param.value]
        }
    }

    @Test
    void jobTypesMapped() {
        // arrange
        def enumValues = JobDefinition.Types.values()

        enumValues.each { type ->
            // act
            def result = JobDefMapper.typeMapping[type]

            // assert
            assertThat "ENUM value ${type}",
                       result,
                       is(not(nullValue()))
        }
    }

    @Test
    void getOracleJobDef() {
        // arrange
        def input = new JobDefinition(jobType: JobDefinition.Types.SyncWebService,
                                      description: 'the desc',
                                      wsdlPath: '/wsdl/path',
                                      service: 'the_service',
                                      port: 'the_port',
                                      operation: 'the_operation',
                                      message: '<message/>',
                                      name: 'the_name')

        // act
        def result = JobDefMapper.getOracleJobDef('http://www.foo.com'.toURL(),
                                                  'theApp',
                                                  input)

        // assert
        assertThat result.name,
                   is(equalTo('the_name'))
        assertThat result.description,
                   is(equalTo('the desc'))
        def jobType = result.jobType
        assertThat jobType.namePart,
                   is(equalTo('SyncWebserviceJobType'))
        assertThat jobType.type,
                   is(equalTo(MetadataObjectId.MetadataObjectType.JOB_TYPE))
        assertThat jobType.packagePart,
                   is(equalTo(JobDefMapper.JOB_TYPE_PACKAGE_FROM_EM))
        def params = getMap(result.parameters)
        assertThat params,
                   is(equalTo([
                           SYS_effectiveApplication: 'theApp',
                           SYS_EXT_wsWsdlBaseUrl   : 'http://www.foo.com',
                           SYS_EXT_wsWsdlUrl       : '/wsdl/path',
                           SYS_EXT_wsServiceName   : 'the_service',
                           SYS_EXT_wsPortName      : 'the_port',
                           SYS_EXT_wsOperationName : 'the_operation',
                           SYS_EXT_invokeMessage   : '<message/>',
                           SYS_externalJobType     : 'SOA'
                   ]))
    }

    @Test
    void getAvioJobDef() {
        // arrange
        def avioInput = new JobDefinition(jobType: JobDefinition.Types.SyncWebService,
                                          description: 'the desc',
                                          wsdlPath: '/wsdl/path',
                                          service: 'the_service',
                                          port: 'the_port',
                                          operation: 'the_operation',
                                          message: '<message/>',
                                          name: 'the_name')
        def oracle = JobDefMapper.getOracleJobDef('http://www.foo.com'.toURL(),
                                                  'theApp',
                                                  avioInput)

        // act
        def avioReverse = JobDefMapper.getAvioJobDefinition(oracle)

        // assert
        assertThat avioReverse,
                   is(equalTo(avioInput))
    }
}
