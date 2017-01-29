package com.avioconsulting.ess.models

import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class PolicySubjectTest {
    @Test
    void getApplication() {
        // arrange
        def subject = new DummyPolicySubject(appName: 'the_app')

        // act
        def result = subject.getApplication('the_domain')

        // assert
        assertThat result,
                   is(equalTo('/WLS/the_domain/the_app'))
    }
}
