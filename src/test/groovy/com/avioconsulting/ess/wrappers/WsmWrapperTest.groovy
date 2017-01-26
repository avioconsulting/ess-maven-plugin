package com.avioconsulting.ess.wrappers

import com.avioconsulting.ess.models.Policy
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class WsmWrapperTest {
    @Test
    void parseExistingPolicies() {
        // arrange
        def output = '\n' +
                'Policy Set Details:\n' +
                '-------------------\n' +
                'Type of Resources:   ESS SOAP JOB Invoker\n' +
                'Scope of Resources:  JOB-INVOKE(\'.\')\n' +
                'Enabled:             true\n' +
                'Policy Reference:    URI=oracle/wsaddr_policy, category=addressing, enabled=true, index=1\n' +
                '                     URI=oracle/log_policy, category=management, enabled=true, index=2\n\n'

        // act
        List<Policy> parsed = WsmWrapper.parseExistingPolicies output

        // assert
        assertThat parsed,
                   is(equalTo([
                           new Policy(name: 'oracle/wsaddr_policy'),
                           new Policy(name: 'oracle/log_policy')
                   ]))
    }
}
