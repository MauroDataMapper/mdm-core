package uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class CreationDiff<C extends Diffable> extends UniDirectionalDiff<C> {

    CreationDiff(Class<C> targetClass) {
        super(targetClass)
    }

    CreationDiff(C created) {
        this(created.getClass() as Class<C>)
        this.value = created
    }

    CreationDiff created(C object) {
        this.value = object
        this
    }

    @Override
    CreationDiff<C> commonAncestor(C ca) {
        this.commonAncestorValue = ca
        this
    }

    @Override
    CreationDiff<C> asMergeConflict() {
        this.mergeConflict = true
        this
    }
}
