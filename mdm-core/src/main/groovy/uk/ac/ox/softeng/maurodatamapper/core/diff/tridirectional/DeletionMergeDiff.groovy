package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class DeletionMergeDiff<D extends Diffable> extends TriDirectionalDiff<D> {

    DeletionMergeDiff(Class<D> targetClass) {
        super(targetClass)
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }

    D getDeleted() {
        super.getValue() as D
    }

    String getDeletedIdentifier() {
        value.diffIdentifier
    }

    DeletionMergeDiff<D> whichDeleted(D object) {
        this.value = object
        this.commonAncestor = object
        this
    }

    DeletionMergeDiff<D> withMergeModification(D modified) {
        super.rightHandSide(modified) as DeletionMergeDiff<D>
    }

    DeletionMergeDiff<D> withNoMergeModification() {
        this
    }

    DeletionMergeDiff<D> withCommonAncestor(D ca) {
        super.withCommonAncestor(ca) as DeletionMergeDiff<D>
    }

    @Override
    DeletionMergeDiff<D> asMergeConflict() {
        super.asMergeConflict() as DeletionMergeDiff<D>
    }

    @Deprecated
    DeletionMergeDiff<D> withSource(D source) {
        super.withSource(source) as DeletionMergeDiff<D>
    }

    @Deprecated
    DeletionMergeDiff<D> withTarget(D target) {
        super.withTarget(target) as DeletionMergeDiff<D>
    }

    @Override
    String toString() {
        "Deleted :: ${deletedIdentifier}"
    }
}
