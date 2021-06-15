package uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class DeletionDiff<D extends Diffable> extends UniDirectionalDiff<D> {

    DeletionDiff(Class<D> targetClass) {
        super(targetClass)
    }

    DeletionDiff(D created) {
        this(created.getClass() as Class<D>)
        this.value = created
    }

    DeletionDiff deleted(D object) {
        this.value = object
        this
    }

    @Override
    DeletionDiff<D> commonAncestor(D ca) {
        this.commonAncestorValue = ca
        this
    }

    @Override
    DeletionDiff<D> asMergeConflict() {
        this.mergeConflict = true
        this
    }
}
