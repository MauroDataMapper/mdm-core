package uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class DeletionDiff<D extends Diffable> extends UniDirectionalDiff<D> {

    DeletionDiff(Class<D> targetClass) {
        super(targetClass)
    }

    D getDeleted() {
        super.getValue() as D
    }

    String getDeletedIdentifier() {
        value.diffIdentifier
    }

    DeletionDiff<D> deleted(D object) {
        this.value = object
        this
    }
}
