package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class CreationMergeDiff<C extends Diffable> extends TriDirectionalDiff<C> {

    CreationMergeDiff(Class<C> targetClass) {
        super(targetClass)
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }

    C getCreated() {
        super.getValue() as C
    }

    String getCreatedIdentifier() {
        created.diffIdentifier
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    CreationMergeDiff whichCreated(C object) {
        withSource(object) as CreationMergeDiff<C>
    }

    CreationMergeDiff<C> withCommonAncestor(C ca) {
        super.withCommonAncestor(ca) as CreationMergeDiff<C>
    }

    CreationMergeDiff<C> withMergeDeletion(C deleted) {
        super.rightHandSide(deleted) as CreationMergeDiff<C>
    }

    CreationMergeDiff<C> withNoMergeDeletion() {
        this
    }

    @Override
    CreationMergeDiff<C> asMergeConflict() {
        super.asMergeConflict() as CreationMergeDiff<C>
    }

    @Deprecated
    CreationMergeDiff<C> withSource(C source) {
        super.withSource(source) as CreationMergeDiff<C>
    }

    @Deprecated
    CreationMergeDiff<C> withTarget(C target) {
        super.withTarget(target) as CreationMergeDiff<C>
    }

    @Override
    String toString() {
        "Created :: ${createdIdentifier}"
    }
}
