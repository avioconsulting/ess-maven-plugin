package com.avioconsulting.ess

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import weblogic.management.scripting.utils.WLSTInterpreter
import weblogic.tools.maven.plugins.configure.WLSTClientMojo

@Mojo(name='foo')
class FooMojo extends WLSTClientMojo {
    void execute() throws MojoExecutionException, MojoFailureException {
        this.log.info('testing out stuff')
        def scriptLines = [
                'connect(url=\'t3://localhost:7001\', username=\'weblogic\', password=\'oracle1234\')',
                'domainRuntime()',
                "manageSchedulerJobDefn('SHOW', 'EssNativeHostingApp')"
        ]
        this.script = scriptLines.join '\n'
        super.execute()
    }
}
