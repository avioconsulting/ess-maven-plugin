package com.avioconsulting.ess

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import weblogic.management.scripting.utils.WLSTInterpreter

@Mojo(name='foo')
class FooMojo extends AbstractMojo {
    void execute() throws MojoExecutionException, MojoFailureException {
        this.log.info('testing out stuff')
        def props = [
                (WLSTInterpreter.ENABLE_SCRIPT_MODE): Boolean.TRUE
        ]
        def interpreter = new WLSTInterpreter(props)
        interpreter.exec('connect(url=\'t3://localhost:7001\', username=\'weblogic\', password=\'oracle1234\')')
        interpreter.exec('disconnect()')
    }
}
