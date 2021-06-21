package uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class CreationDiff<C extends Diffable> extends UniDirectionalDiff<C> {

    CreationDiff(Class<C> targetClass) {
        super(targetClass)
    }

    C getCreated() {
        super.getValue() as C
    }

    String getCreatedIdentifier() {
        value.diffIdentifier
    }

    CreationDiff created(C object) {
        this.value = object
        this
    }
}
