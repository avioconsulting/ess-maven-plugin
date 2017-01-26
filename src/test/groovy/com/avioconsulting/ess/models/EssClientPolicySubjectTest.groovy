package com.avioconsulting.ess.models

import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class EssClientPolicySubjectTest {
    @Test
    void getApplicationNameOnly_default() {
        // arrange
        def subject = new EssClientPolicySubject(jobDefinition: null)

        // act
        def result = subject.applicationNameOnly

        // assert
        assertThat result,
                   is(equalTo('EssNativeHostingApp'))
    }

    @Test
    void getApplicationNameOnly_specified() {
        // arrange
        def subject = new EssClientPolicySubject(jobDefinition: null,
                                                 essHostApplicationName: 'foo')

        // act
        def result = subject.applicationNameOnly

        // assert
        assertThat result,
                   is(equalTo('foo'))
    }

    @Test
    void getAssembly() {
        // arrange
        def jobDef = new JobDefinition(jobType: JobDefinition.Types.SyncWebService,
                                       description: 'the desc',
                                       wsdlPath: '/wsdl/path',
                                       service: 'the_service',
                                       port: 'the_port',
                                       operation: 'the_operation',
                                       message: '<message/>',
                                       name: 'the_name')
        def subject = new EssClientPolicySubject(jobDefinition: jobDef)

        // act
        def result = subject.assembly

        // assert
        assertThat result,
                   is(equalTo('%WsmPolicy://oracle/apps/ess/custom/the_name'))
    }
}
