package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.factories.PolicyAttachmentFactory
import com.avioconsulting.ess.models.Policy
import com.avioconsulting.ess.wrappers.WsmWrapper
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = 'attachPolicies')
class WsmMojo extends CommonMojo {
    private static final List<Policy> DEFAULT_ESS_POLICIES = [new Policy(name: 'oracle/wsaddr_policy')]

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
        def wsmWrapper = new WsmWrapper(this.adminServerURL,
                                        this.weblogicUser,
                                        this.weblogicPassword,
                                        this.wrapperLogger)
        policyAttachments.each { attach ->
            wsmWrapper.begin()
            def subject = attach.policySubject
            this.log.info "Policy subject ${subject.assembly}"
            def existing = wsmWrapper.getExistingPolicies(subject)
            def desiredPolicies = attach.policies
            // don't want to remove default policy
            def removePolicies = existing - desiredPolicies - DEFAULT_ESS_POLICIES
            removePolicies.each { policy ->
                this.log.info "Detaching policy ${policy}"
                wsmWrapper.detachPolicy(subject, policy)
            }
            def createPolicies = desiredPolicies - existing
            createPolicies.each { policy ->
                this.log.info "Attaching policy ${policy}"
                wsmWrapper.attachPolicy(subject, policy)
            }
            if (removePolicies.empty && createPolicies.empty) {
                this.log.info 'No policy additions/deletions required!'
            }
            wsmWrapper.commit()
        }
        wsmWrapper.close()
    }
}
