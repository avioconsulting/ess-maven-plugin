package com.avioconsulting.ess.wrappers

import com.avioconsulting.EssPolicyNotifier
import com.avioconsulting.util.Logger
import weblogic.jndi.WLInitialContextFactory

import javax.naming.Context
import javax.naming.InitialContext

class EssPolicyFixer {
    private final String soaWeblogicUrl
    private final String weblogicUser
    private final String weblogicPassword
    private final Logger logger

    EssPolicyFixer(String soaWeblogicUrl,
                   String weblogicUser,
                   String weblogicPassword,
                   Logger logger) {

        this.logger = logger
        this.weblogicPassword = weblogicPassword
        this.weblogicUser = weblogicUser
        this.soaWeblogicUrl = soaWeblogicUrl
    }

    def createPolicyAssembly(String hostingApp, String essPackage, String jobName) {
        withRemote { EssPolicyNotifier notifier ->
            notifier.createPolicyAssembly(hostingApp, essPackage, jobName)
        }
    }

    def deletePolicyAssembly(String hostingApp, String essPackage, String jobName) {
        withRemote { EssPolicyNotifier notifier ->
            notifier.deletePolicyAssembly(hostingApp, essPackage, jobName)
        }
    }

    private withRemote(Closure closure) {
        withContext { Context context ->
            EssPolicyNotifier notifier = context.lookup(
                    'java:global/ess-policy-notifier/EssPolicyNotifierModule/EssPolicyNotifierBean') as EssPolicyNotifier
            List<String> messages = closure(notifier)
            messages.each { msg ->
                this.logger.info(msg)
            }
        }
    }

    private withContext(Closure closure) {
        Hashtable<String, String> props = [
                (Context.INITIAL_CONTEXT_FACTORY): WLInitialContextFactory.name,
                (Context.PROVIDER_URL)           : this.soaWeblogicUrl,
                (Context.SECURITY_PRINCIPAL)     : this.weblogicUser,
                (Context.SECURITY_CREDENTIALS)   : this.weblogicPassword
        ]
        def context = new InitialContext(props)
        closure(context)
        context.close()
    }
}
