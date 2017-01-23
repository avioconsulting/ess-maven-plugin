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

    protected static validateName(String name) {
        def errors = []
        if (name.contains(' ')) {
            errors << 'spaces'
        }
        if (name =~ /^[0-9].*/) {
            errors << 'leading numbers'
        }
        if (!(name =~ /^[a-zA-Z][a-zA-Z0-9_]*$/)) {
            errors << 'special characters'
        }
        if (errors.any()) {
            throw new Exception("Name value '${name}' not allowed. ESS does not allow ${errors.join(', ')}!")
        }
        true
    }
}
