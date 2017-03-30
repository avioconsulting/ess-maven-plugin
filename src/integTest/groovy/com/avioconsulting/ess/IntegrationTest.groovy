package com.avioconsulting.ess

import com.avioconsulting.ess.factories.JobDefinitionFactory
import com.avioconsulting.ess.models.EssClientPolicySubject
import com.avioconsulting.ess.mojos.CommonMojo
import com.avioconsulting.ess.mojos.FieldConstants
import com.avioconsulting.ess.mojos.JobScheduleMojo
import com.avioconsulting.ess.wrappers.MetadataServiceWrapper
import org.junit.Before
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

@SuppressWarnings("GroovyAccessibility")
class IntegrationTest {
    static final String hostname = System.getProperty('weblogic.hostname') ?: 'localhost'
    static final String port = System.getProperty('weblogic.port') ?: '8001'
    static final String username = System.getProperty('weblogic.username') ?: 'weblogic'
    static final String password = System.getProperty('weblogic.password') ?: 'oracle1234'
    private Map<Class, List<Class>> factories

    static def setFields(CommonMojo mojo, Map fields, Class overrideClass = null) {
        def klass = overrideClass ?: mojo.class
        fields.each { key, value ->
            def field = klass.getDeclaredField(key as String)
            field.accessible = true
            field.set(mojo, value)
        }
    }

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

    @Before
    void cleanup() {
        this.factories = [:]
        def mojo = getMojo()
        mojo.cleanFirst = true
        mojo.execute()
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

    private void mockDiscoveredFactories(CommonMojo mojo) {
        mojo.metaClass.getSubTypesOf = { Class intf ->
            this.factories.containsKey(intf) ? this.factories[intf] : []
        }
    }

    private static setCommonMojoFields(CommonMojo mojo) {
        def url = "t3://${hostname}:${port}".toString()
        setFields mojo, [
                soaWeblogicUrl      : url,
                weblogicUser        : username,
                weblogicPassword    : password,
                configurationPackage: Factory.package.name,
                essHostingApp       : EssClientPolicySubject.DEFAULT_ESS_HOST_APP,
                essDeployPackage    : MetadataServiceWrapper.DEFAULT_ESS_DEPLOY_PACKAGE
        ], CommonMojo
    }
}
