package com.avioconsulting.ess

import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class JobDefDeployerTest {
    def parseDefinitions(rawInput) {
        new JobDefDeployer(null, 'EssNativeHostingApp').parseDefinitions(rawInput)
    }

    @Test
    void parseDefinitions_Multiple() {
        // arrange
        def raw = 'query =  [ESSAPP:type=Scheduler,Name=MetadataService,Application=ESSAPP,Location=soa_server1]\nreturning object name:  ESSAPP:type=Scheduler,Name=MetadataService,Application=ESSAPP,Location=soa_server1\nJob Definitions present in namespace of "EssNativeHostingApp" are: \ntest: JobDefinition://oracle/apps/ess/custom/test\ntest2: JobDefinition://oracle/apps/ess/custom/test2\n'

        // act
        def result = parseDefinitions raw

        // assert
        assertThat result,
                   is(equalTo(['test', 'test2']))
    }

    @Test
    void parseDefinitions_Single() {
        // arrange
        def raw = 'query =  [ESSAPP:type=Scheduler,Name=MetadataService,Application=ESSAPP,Location=soa_server1]\nreturning object name:  ESSAPP:type=Scheduler,Name=MetadataService,Application=ESSAPP,Location=soa_server1\nJob Definitions present in namespace of "EssNativeHostingApp" are: \ntest: JobDefinition://oracle/apps/ess/custom/test\n'

        // act
        def result = parseDefinitions raw

        // assert
        assertThat result,
                   is(equalTo(['test']))
    }

    @Test
    void parseDefinitions_None() {
        // arrange
        def raw = 'query =  [ESSAPP:type=Scheduler,Name=MetadataService,Application=ESSAPP,Location=soa_server1]\nreturning object name:  ESSAPP:type=Scheduler,Name=MetadataService,Application=ESSAPP,Location=soa_server1\nNo Job Definitions present in namespace of "EssNativeHostingApp". \n'

        // act
        def result = parseDefinitions raw

        // assert
        assertThat result,
                   is(equalTo([]))
    }

    @Test
    void getProperties() {
        // arrange
        def jobDefinition = new JobDefinition(JobDefinition.Types.SyncWebserviceJobType,
                                              'the desc',
                                              'http://www.foo.com/wsdl/path'.toURL(),
                                              'the_service',
                                              'the_port',
                                              'the_operation',
                                              '<message/>',
                                              'the name')

        // act
        def result = new JobDefDeployer(null, 'EssNativeHostingApp').getProperties(jobDefinition)

        // assert
        assertThat result,
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

    @Test
    void getPythonDict() {
        // arrange
        def input = [foo: '123', bar: 456]

        // act
        def result = JobDefDeployer.getPythonDict(input)

        // assert
        assertThat result,
                   is(equalTo("{'foo': '123', 'bar': '456'}"))
    }
}
