package com.avioconsulting.ess.mojos

import com.avioconsulting.ess.factories.PolicyAttachmentFactory
import com.avioconsulting.ess.models.Policy
import com.avioconsulting.util.EssPolicyFixer
import com.avioconsulting.ess.wrappers.WsmWrapper
import com.avioconsulting.util.PythonCaller
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@SuppressWarnings("GroovyUnusedDeclaration")
@Mojo(name = 'attachPolicies')
class WsmMojo extends CommonMojo {
    private static final List<Policy> DEFAULT_ESS_POLICIES = [new Policy(name: 'oracle/wsaddr_policy')]

    @Parameter(property = 'admin.t3.url', required = true)
    private String adminServerURL

    @Parameter(property = 'ess.target', defaultValue = 'soa_cluster')
    private String essTarget

    void execute() throws MojoExecutionException, MojoFailureException {
        def reflections = getReflectionsUtility()
        def policyAttachments = reflections.getSubTypesOf(PolicyAttachmentFactory).collect { klass ->
            klass.newInstance().createPolicyAttachment()
        }
        if (!policyAttachments.any()) {
            this.log.info 'No policies to load'
            return
        }
        def caller = new PythonCaller()
        caller.methodCall('connect', [
                url     : this.adminServerURL,
                username: weblogicUser,
                password: weblogicPassword
        ])
        def essFixer = new EssPolicyFixer(this.soaWeblogicUrl,
                                          this.weblogicUser,
                                          this.weblogicPassword,
                                          this.essTarget,
                                          caller,
                                          this.wrapperLogger)
        def wsmWrapper = new WsmWrapper(caller,
                                        essFixer,
                                        this.wrapperLogger,
                                        this.essDeployPackage)
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
        caller.methodCall('disconnect')
    }
}
