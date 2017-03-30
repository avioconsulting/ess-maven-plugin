package com.avioconsulting.ess

import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.mojos.FieldConstants
import com.avioconsulting.ess.mojos.JobScheduleMojo
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

@SuppressWarnings("GroovyAccessibility")
class JobDefinitionTest extends Common {
    JobScheduleMojo getMojo() {
        def mojo = new JobScheduleMojo()
        setCommonMojoFields(mojo)
        mockDiscoveredFactories(mojo)
        def url = "http://${hostname}:${port}".toString()
        setFields mojo,
                  [
                          soaDeployUrl      : url,
                          essMetadataEjbJndi: FieldConstants.ESS_JNDI_EJB_METADATA,
                          essRuntimeEjbJndi : FieldConstants.ESS_JNDI_EJB_RUNTIME,
                          serverTimeZone    : 'America/Denver'
                  ]
        mojo
    }

    @Test
    void NoExistingJobDef_CreatesWithoutError() {
        // arrange
        this.factories[JobDefinitionFactory] = [SingleJobDefFactory]
        def mojo = getMojo()

        // act
        mojo.execute()

        // assert
        def expectedJobDef = new SingleJobDefFactory().createJobDefinition()
        assertThat mojo.newJobDefs,
                   is(equalTo([expectedJobDef]))
        assertThat mojo.updateJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(empty())
    }

    @Test
    void ExistingJobDef_Same_CreatesWithoutError() {
        // arrange
        this.factories[JobDefinitionFactory] = [SingleJobDefFactory]
        def mojo = getMojo()
        mojo.execute()
        mojo = getMojo()

        // act
        mojo.execute()

        // assert
        assertThat mojo.updateJobDefs,
                   is(empty())
        assertThat mojo.newJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(empty())
    }

    @Test
    void ExistingJobDef_Changed_UpdatesWithoutError() {
        // arrange
        this.factories[JobDefinitionFactory] = [SingleJobDefFactory]
        def mojo = getMojo()
        mojo.execute()
        this.factories[JobDefinitionFactory] = [UpdatedJobDefFactory]
        mojo = getMojo()

        // act
        mojo.execute()

        // assert
        def expectedJobDef = new UpdatedJobDefFactory().createJobDefinition()
        assertThat mojo.updateJobDefs,
                   is(equalTo([expectedJobDef]))
        assertThat mojo.newJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(empty())
    }
}
