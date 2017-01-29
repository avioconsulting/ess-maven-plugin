package com.avioconsulting.ess.wrappers

import com.avioconsulting.ess.models.Policy
import groovy.test.GroovyAssert
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

    @Test
    void parseExistingPolicies_Overrides_One() {
        // arrange
        def output = '\n' +
                'Policy Set Details:\n' +
                '-------------------\n' +
                'Type of Resources:   ESS SOAP JOB Invoker\n' +
                'Scope of Resources:  JOB-INVOKE(\'.\')\n' +
                'Enabled:             true\n' +
                'Policy Reference:    URI=oracle/wsaddr_policy, category=addressing, enabled=true, index=1\n' +
                '                     URI=oracle/wss_username_token_client_policy, category=security, enabled=true, index=2\n' +
                '                     csf-key=foobar.credentials\n\n'

        // act
        List<Policy> parsed = WsmWrapper.parseExistingPolicies output

        // assert
        assertThat parsed,
                   is(equalTo([
                           new Policy(name: 'oracle/wsaddr_policy'),
                           new Policy(name: 'oracle/wss_username_token_client_policy',
                                      overrides: ['csf-key': 'foobar.credentials'])
                   ]))
    }

    @Test
    void parseExistingPolicies_Overrides_Two() {
        // arrange
        def output = '\n' +
                'Policy Set Details:\n' +
                '-------------------\n' +
                'Type of Resources:   ESS SOAP JOB Invoker\n' +
                'Scope of Resources:  JOB-INVOKE(\'.\')\n' +
                'Enabled:             true\n' +
                'Policy Reference:    URI=oracle/wsaddr_policy, category=addressing, enabled=true, index=1\n' +
                '                     URI=oracle/wss_username_token_client_policy, category=security, enabled=true, index=2\n' +
                '                     csf-key=foobar.credentials\n' +
                '                     csf.map=howdy\n\n'

        // act
        List<Policy> parsed = WsmWrapper.parseExistingPolicies output

        // assert
        assertThat parsed,
                   is(equalTo([
                           new Policy(name: 'oracle/wsaddr_policy'),
                           new Policy(name: 'oracle/wss_username_token_client_policy',
                                      overrides: ['csf-key': 'foobar.credentials',
                                                  'csf.map': 'howdy'])
                   ]))
    }

    @Test
    void parseOverrideOutput_Added() {
        // arrange
        def output = '\n' +
                'The configuration override property "csf.map" having value "howdy" has been added to the reference to policy with URI "oracle/wss_username_token_client_policy".\n'

        // act
        def result = WsmWrapper.parseOverrideOutput(output, 'csf.map')

        // assert
        assertThat result,
                   is(equalTo(true))
    }

    @Test
    void parseOverrideOutput_Updated() {
        // arrange
        def output = '\'\n' +
                'The value for configuration override property "csf-key" specified within the reference to policy with URI "oracle/wss_username_token_client_policy" have been updated to "foobar.credentials".\n'

        // act
        def result = WsmWrapper.parseOverrideOutput(output, 'csf-key')

        // assert
        assertThat result,
                   is(equalTo(true))
    }

    @Test
    void parseOverrideOutput_Error() {
        // arrange
        def output = '\'\n' +
                'A reference to the "foobar" policy was not found in the policy set.\n'

        // act + assert
        GroovyAssert.shouldFail {
            WsmWrapper.parseOverrideOutput(output, 'csf-key')
        }
    }
}
