package uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class DeletionDiff<D extends Diffable> extends UniDirectionalDiff<D> {

    DeletionDiff(Class<D> targetClass) {
        super(targetClass)
    }

    DeletionDiff deleted(D object) {
        this.value = object
        this
    }

    DeletionDiff<D> commonAncestor(D ca) {
        super.commonAncestor(ca) as DeletionDiff<D>
    }

    @Override
    DeletionDiff<D> asMergeConflict() {
        super.asMergeConflict() as DeletionDiff<D>
    }
}
