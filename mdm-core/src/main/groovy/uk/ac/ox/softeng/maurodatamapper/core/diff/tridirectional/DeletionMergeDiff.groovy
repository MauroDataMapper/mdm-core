package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff

import groovy.transform.CompileStatic

@CompileStatic
class DeletionMergeDiff<D extends Diffable> extends TriDirectionalDiff<D> {

    ObjectDiff<D> mergeModificationDiff

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

    boolean isSourceDeletionAndTargetModification() {
        mergeModificationDiff != null
    }

    DeletionMergeDiff<D> whichDeleted(D object) {
        this.value = object
        withCommonAncestor object
    }

    DeletionMergeDiff<D> withMergeModification(ObjectDiff<D> modifiedDiff) {
        this.mergeModificationDiff = modifiedDiff
        this
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
        String str = "Deleted :: ${deletedIdentifier}"
        mergeModificationDiff ? "${str}\n    >> Modified :: ${mergeModificationDiff}" : str
    }
}
