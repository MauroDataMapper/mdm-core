package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

@CompileStatic
class CreationMergeDiff<C extends Diffable> extends TriDirectionalDiff<C> implements Comparable<CreationMergeDiff> {

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

    String getFullyQualifiedPath() {
        String cleanedIdentifier = createdIdentifier.split('/').last()
        "${fullyQualifiedObjectPath}|${created.pathPrefix}:${cleanedIdentifier}"
    }

    boolean isSourceModificationAndTargetDeletion() {
        commonAncestor != null
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    CreationMergeDiff<C> whichCreated(C object) {
        withSource(object) as CreationMergeDiff<C>
    }

    CreationMergeDiff<C> insideFullyQualifiedObjectPath(String fullyQualifiedObjectPath) {
        super.insideFullyQualifiedObjectPath(fullyQualifiedObjectPath) as CreationMergeDiff<C>
    }

    CreationMergeDiff<C> withCommonAncestor(C ca) {
        super.withCommonAncestor(ca) as CreationMergeDiff<C>
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

    @Override
    int compareTo(CreationMergeDiff that) {
        this.createdIdentifier <=> that.createdIdentifier
    }
}
