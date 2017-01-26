package com.avioconsulting.ess.models

class DummyPolicySubject extends PolicySubject {
    String appName

    DummyPolicySubject(Map map) {
        super(map)
    }

    String getApplicationNameOnly() {
        appName
    }

    String getAssembly() {
        return null
    }

    String getSubject() {
        return null
    }
}
