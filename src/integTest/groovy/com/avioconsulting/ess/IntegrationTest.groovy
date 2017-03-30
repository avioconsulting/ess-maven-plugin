package com.avioconsulting.ess

import com.avioconsulting.ess.models.EssClientPolicySubject
import com.avioconsulting.ess.mojos.CommonMojo
import com.avioconsulting.ess.mojos.FieldConstants
import com.avioconsulting.ess.mojos.JobScheduleMojo
import com.avioconsulting.ess.wrappers.MetadataServiceWrapper
import org.junit.Before
import org.junit.Test
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

class IntegrationTest {
    static final String hostname = System.getProperty('weblogic.hostname') ?: 'localhost'
    static final String port = System.getProperty('weblogic.port') ?: '8001'
    static final String username = System.getProperty('weblogic.username') ?: 'weblogic'
    static final String password = System.getProperty('weblogic.password') ?: 'oracle1234'

    def setFields(CommonMojo mojo, Map fields, Class overrideClass = null) {
        def klass = overrideClass ?: mojo.class
        fields.each { key, value ->
            def field = klass.getDeclaredField(key)
            field.accessible = true
            field.set(mojo, value)
        }
    }

    JobScheduleMojo getMojo(Class factory) {
        def mojo = new JobScheduleMojo()
        setCommonMojoFields(mojo)
        setupReflections(mojo, factory)
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
        // do a noop factory for cleaning
        def mojo = getMojo(String)
        mojo.cleanFirst = true
        mojo.execute()
    }

    @Test
    void NoExistingJobDef() {
        // arrange
        def mojo = getMojo(IntegrationFactory)

        // act
        mojo.execute()

        // assert
        def expectedJobDef = new IntegrationFactory().createJobDefinition()
        assertThat mojo.newJobDefs,
                   is(equalTo([expectedJobDef]))
        assertThat mojo.updateJobDefs,
                   is(empty())
        assertThat mojo.canceledJobDefs,
                   is(empty())
    }

    private void setupReflections(CommonMojo mojo, Class factory) {
        def config = new ConfigurationBuilder()
                .forPackages(factory.package.name)
        def reflections = new Reflections(config)
        mojo.metaClass.getReflectionsUtility = {
            reflections
        }
    }

    private setCommonMojoFields(CommonMojo mojo) {
        def url = "t3://${hostname}:${port}".toString()
        setFields mojo, [
                soaWeblogicUrl      : url,
                weblogicUser        : username,
                weblogicPassword    : password,
                configurationPackage: IntegrationFactory.package.name,
                essHostingApp       : EssClientPolicySubject.DEFAULT_ESS_HOST_APP,
                essDeployPackage    : MetadataServiceWrapper.DEFAULT_ESS_DEPLOY_PACKAGE
        ], CommonMojo
    }
}
