package com.avioconsulting.ess

class Configuration {
    final List<JobDefinition> jobDefinitions
    final String hostingApplication

    Configuration() {
        this.jobDefinitions = new ArrayList<JobDefinition>()
        // default
        this.hostingApplication = 'EssNativeHostingApp'
    }
}