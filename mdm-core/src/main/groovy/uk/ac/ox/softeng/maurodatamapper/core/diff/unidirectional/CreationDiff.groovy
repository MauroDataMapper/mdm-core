package uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class CreationDiff<C extends Diffable> extends UniDirectionalDiff<C> {

    CreationDiff(Class<C> targetClass) {
        super(targetClass)
    }

    CreationDiff created(C object) {
        this.value = object
        this
    }

    CreationDiff<C> commonAncestor(C ca) {
        super.commonAncestor(ca) as CreationDiff<C>
    }

    @Override
    CreationDiff<C> asMergeConflict() {
        super.asMergeConflict() as CreationDiff<C>
    }
}
