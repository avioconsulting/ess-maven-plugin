package com.avioconsulting.ess.models

class BaseModel {
    BaseModel(Map map) {
        // TODO: Groovy AST, smarter transform for this
        def allFields = this.class.declaredFields.collect { f -> f.name }
                .findAll { f -> !f.startsWith('_') && !f.startsWith('$') && f != 'metaClass' } // hidden fields
        def missingFields = allFields - map.keySet()
        if (missingFields.any()) {
            throw new Exception("The following fields are missing from your constructor!: ${missingFields}")
        }
        map.each { k, v ->
            try {
                this."${k}" = v
            }
            catch (e) {
                throw new Exception("Problem with ${k}", e)
            }
        }
    }
}
