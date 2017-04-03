package com.avioconsulting.ess

import com.avioconsulting.ess.factories.JobDefinitionFactory
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

@SuppressWarnings("GroovyAccessibility")
class JobDefinitionTest extends Common {
    @Test
    void NoExistingJobDef_CreatesWithoutError() {
        // arrange
        this.factories[JobDefinitionFactory] = [SingleJobDefFactory]
        def mojo = getJobScheduleMojo()

        // act
        mojo.execute()

        // assert
        def expectedJobDef = new SingleJobDefFactory().createJobDefinition()
        assertThat mojo.newJobDefs,
                   is(equalTo([expectedJobDef]))
        assertThat mojo.updatedJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(empty())
    }

    @Test
    void ExistingJobDef_Same_CreatesWithoutError() {
        // arrange
        this.factories[JobDefinitionFactory] = [SingleJobDefFactory]
        def mojo = getJobScheduleMojo()
        mojo.execute()
        mojo = getJobScheduleMojo()

        // act
        mojo.execute()

        // assert
        assertThat mojo.updatedJobDefs,
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
        def mojo = getJobScheduleMojo()
        mojo.execute()
        this.factories[JobDefinitionFactory] = [UpdatedJobDefFactory]
        mojo = getJobScheduleMojo()

        // act
        mojo.execute()

        // assert
        def expectedJobDef = new UpdatedJobDefFactory().createJobDefinition()
        assertThat mojo.updatedJobDefs,
                   is(equalTo([expectedJobDef]))
        assertThat mojo.newJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(empty())
    }
}
