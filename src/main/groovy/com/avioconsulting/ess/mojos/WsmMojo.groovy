package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.factories.PolicyAttachmentFactory
import com.avioconsulting.ess.wrappers.WsmWrapper
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = 'attachPolicies')
class WsmMojo extends CommonMojo {
    @Parameter(property = 'admin.t3.url', required = true)
    private String adminServerURL

    void execute() throws MojoExecutionException, MojoFailureException {
        def reflections = getReflectionsUtility()
        def policyAttachments = reflections.getSubTypesOf(PolicyAttachmentFactory).collect { klass ->
            klass.newInstance().createPolicyAttachment()
        }
        if (!policyAttachments.any()) {
            this.log.info 'No policies to load'
            return
        }
        this.log.info "Policies are ${policyAttachments}"
        def wsmWrapper = new WsmWrapper(this.adminServerURL, this.weblogicUser, this.weblogicPassword)
        wsmWrapper.close()
    }
}
