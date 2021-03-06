package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.models.EssClientPolicySubject
import com.avioconsulting.ess.wrappers.MetadataServiceWrapper
import com.avioconsulting.util.Logger
import com.avioconsulting.util.MavenLogger
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

abstract class CommonMojo extends AbstractMojo {
    @Parameter(property = 'soa.t3.url', required = true)
    protected String soaWeblogicUrl

    @Parameter(property = 'weblogic.user', required = true)
    protected String weblogicUser

    @Parameter(property = 'weblogic.password', required = true)
    protected String weblogicPassword

    @Parameter(property = 'ess.config.package', required = true)
    private String configurationPackage

    @Parameter(property = 'ess.host.app', defaultValue = EssClientPolicySubject.DEFAULT_ESS_HOST_APP)
    protected String essHostingApp

    @Parameter(property = 'ess.deploy.package', defaultValue = MetadataServiceWrapper.DEFAULT_ESS_DEPLOY_PACKAGE)
    protected String essDeployPackage

    @Component
    private MavenProject project

    private reflections = null

    protected Set<Class> getSubTypesOf(Class klass) {
        if (this.reflections == null) {
            // artifacts from our project, which is where the configuration is, won't be in the classpath by default
            ClasspathHelper.contextClassLoader().addURL(this.project.artifact.file.toURL())
            // used more complex config/construction due to
            def configBuilder = new ConfigurationBuilder()
                    .addClassLoader(ClasspathHelper.contextClassLoader())
                    .addClassLoader(ClasspathHelper.staticClassLoader())
                    .addUrls(this.project.artifact.file.toURL())
                    .forPackages(this.configurationPackage)
            this.reflections = new Reflections(configBuilder)
        }
        this.reflections.getSubTypesOf(klass)
    }

    protected Logger getWrapperLogger() {
        new MavenLogger(this.log)
    }
}
