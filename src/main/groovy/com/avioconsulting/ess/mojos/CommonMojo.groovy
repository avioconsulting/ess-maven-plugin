package com.avioconsulting.ess.mojos

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

abstract class CommonMojo extends AbstractMojo {
    @Parameter(property = 'weblogic.user', required = true)
    protected String weblogicUser

    @Parameter(property = 'weblogic.password', required = true)
    protected String weblogicPassword

    @Parameter(property = 'ess.config.package', required = true)
    private String configurationPackage

    @Parameter(property = 'ess.host.app', defaultValue = 'EssNativeHostingApp')
    protected String essHostingApp

    @Component
    private MavenProject project

    protected getReflectionsUtility() {
        // artifacts from our project, which is where the configuration is, won't be in the classpath by default
        ClasspathHelper.contextClassLoader().addURL(this.project.artifact.file.toURL())
        // used more complex config/construction due to
        def configBuilder = new ConfigurationBuilder()
                .addClassLoader(ClasspathHelper.contextClassLoader())
                .addClassLoader(ClasspathHelper.staticClassLoader())
                .addUrls(this.project.artifact.file.toURL())
                .forPackages(this.configurationPackage)
        new Reflections(configBuilder)
    }
}
