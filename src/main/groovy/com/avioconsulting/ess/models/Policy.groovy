package com.avioconsulting.ess.models

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

@Canonical
@InheritConstructors
class Policy extends BaseModel {
    String name

    Policy(Map map) {
        super(map)
    }
}
