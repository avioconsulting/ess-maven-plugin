package com.avioconsulting.util

import com.avioconsulting.EssPolicyNotifier
import org.apache.commons.io.IOUtils
import org.python.core.Py
import org.python.core.PyObject
import weblogic.jndi.WLInitialContextFactory

import javax.naming.Context
import javax.naming.InitialContext

class EssPolicyFixer {
    private final String soaWeblogicUrl
    private final String weblogicUser
    private final String weblogicPassword
    private final Logger logger
    private final PythonCaller pythonCaller

    EssPolicyFixer(String soaWeblogicUrl,
                   String weblogicUser,
                   String weblogicPassword,
                   String essTarget,
                   PythonCaller pythonCaller,
                   Logger logger) {

        this.pythonCaller = pythonCaller
        this.logger = logger
        this.weblogicPassword = weblogicPassword
        this.weblogicUser = weblogicUser
        this.soaWeblogicUrl = soaWeblogicUrl
        def props = new Properties()
        // get version we were built with
        props.load(getClass().getResourceAsStream('/version.properties'))
        String version = props.get('version')
        String appName = "ess-policy-notifier#${version}"
        if (!isAppDeployed(appName)) {
            deployApplication(appName,
                              essTarget)
        }
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

        // TODO: Check if the application is deployed, if it's not, deploy it
        withContext { Context context ->
            EssPolicyNotifier notifier = context.lookup(
                    'java:global/ess-policy-notifier/EssPolicyNotifierModule/EssPolicyNotifierBean') as EssPolicyNotifier
            List<String> messages = closure(notifier)
            messages.each { msg ->
                this.logger.info(msg)
            }
        }
    }

    private deployApplication(String applicationName, String essTarget) {
        this.logger.info "Deploying ESS fixer app ${applicationName}..."
        InputStream ear = getClass().getResourceAsStream('/ess-policy-notifier.ear')
        def tempFile = File.createTempFile('app', '.ear')
        tempFile.deleteOnExit()
        def outputStream = new FileOutputStream(tempFile)
        IOUtils.copy(ear, outputStream)
        outputStream.flush()
        outputStream.close()
        ear.close()
        this.pythonCaller.methodCall('deploy',
                                     [
                                             applicationName,
                                             tempFile.absolutePath
                                     ],
                                     [
                                             targets: essTarget,
                                             block  : 'true',
                                             upload : 'true'
                                     ])
    }

    private boolean isAppDeployed(String appName) {
        def caller = this.pythonCaller
        caller.methodCall('cd', ['/AppDeployments'])
        PyObject result = null
        // don't care about the output
        caller.withInterceptedStdout {
            result = caller.methodCall('ls', [returnMap: 'true'])
        }
        List deployedApps = Py.tojava(result, ArrayList)
        caller.methodCall('cd', ['/'])
        deployedApps.contains(appName)
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
