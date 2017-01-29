package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class Policy extends BaseModel {
    String name
    Map<String, String> overrides

    Policy(Map map) {
        // overrides are not mandatory
        super(map << [overrides: (map.overrides ?: [:])])
    }
}
