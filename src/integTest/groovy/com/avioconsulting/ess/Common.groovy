package com.avioconsulting.ess

import com.avioconsulting.ess.models.EssClientPolicySubject
import com.avioconsulting.ess.mojos.CommonMojo
import com.avioconsulting.ess.wrappers.MetadataServiceWrapper
import org.junit.Before

abstract class Common {
    static final String hostname = System.getProperty('weblogic.hostname') ?: 'localhost'
    static final String port = System.getProperty('weblogic.port') ?: '8001'
    static final String username = System.getProperty('weblogic.username') ?: 'weblogic'
    static final String password = System.getProperty('weblogic.password') ?: 'oracle1234'
    protected Map<Class, List<Class>> factories

    static def setFields(CommonMojo mojo, Map fields, Class overrideClass = null) {
        def klass = overrideClass ?: mojo.class
        fields.each { key, value ->
            def field = klass.getDeclaredField(key as String)
            field.accessible = true
            field.set(mojo, value)
        }
    }

    @Before
    void cleanup() {
        this.factories = [:]
        def mojo = getMojo()
        mojo.cleanFirst = true
        mojo.execute()
    }

    abstract CommonMojo getMojo()

    protected void mockDiscoveredFactories(CommonMojo mojo) {
        mojo.metaClass.getSubTypesOf = { Class intf ->
            this.factories.containsKey(intf) ? this.factories[intf] : []
        }
    }

    protected static setCommonMojoFields(CommonMojo mojo) {
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
