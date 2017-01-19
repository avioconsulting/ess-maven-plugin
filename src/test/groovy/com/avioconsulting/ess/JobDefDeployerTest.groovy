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
}
