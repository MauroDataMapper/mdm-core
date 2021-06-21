package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.CreationDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.DeletionDiff

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.arrayMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.creationDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.creationMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.deletionDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.deletionMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.fieldMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.mergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.objectDiff

/**
 * Holds the  result of 2 diffs which provides a unified diff of the changes which one object has made which another object has not made
 * based off a common ancestor
 *
 * The final diff should be the intent of the changes to merge the LHS/source INTO the LHS/target
 *
 * See <a href="https://metadatacatalogue.myjetbrains.com/youtrack/issue/MC-9228">YouTrack MC-9228</a> for rules
 *
 * <pre>
 *  source                                  target
 *    ^                                     ^
 *     \                                   /
 *   caSourceObjectDiff / source     caTargetObjectDiff / target
 *           \                           /
 *            \                         /
 *                  commonAncestor
 * </pre>
 */
@Slf4j
class MergeDiff<M extends Diffable> extends TriDirectionalDiff<M> {

    List<FieldMergeDiff> diffs

    ObjectDiff<M> commonAncestorDiffSource
    ObjectDiff<M> commonAncestorDiffTarget

    MergeDiff(Class<M> targetClass) {
        super(targetClass)
        diffs = []
    }

    @Override
    Integer getNumberOfDiffs() {
        diffs?.sum { it.getNumberOfDiffs() } as Integer ?: 0
    }

    MergeDiff<M> forMergingDiffable(M sourceSide) {
        super.withSource(sourceSide) as MergeDiff<M>
    }

    MergeDiff<M> intoDiffable(M targetSide) {
        super.withTarget(targetSide) as MergeDiff<M>
    }

    MergeDiff<M> havingCommonAncestor(M ca) {
        super.withCommonAncestor(ca) as MergeDiff<M>
    }

    MergeDiff<M> withCommonAncestorDiffedAgainstSource(ObjectDiff<M> commonAncestorDiffSource) {
        this.commonAncestorDiffSource = commonAncestorDiffSource
        this
    }

    MergeDiff<M> withCommonAncestorDiffedAgainstTarget(ObjectDiff<M> commonAncestorDiffTarget) {
        this.commonAncestorDiffTarget = commonAncestorDiffTarget
        this
    }

    MergeDiff<M> append(FieldMergeDiff fieldDiff) {
        diffs.add(fieldDiff)
        this
    }

    /**
     * The resulting MergeDiff should display all the actual differences including merge conflicts with the intent of merging the LHS/source into the
     * RHS/target.
     *
     */
    @SuppressWarnings('GroovyVariableNotAssigned')
    MergeDiff<M> generate() {
        log.debug('Generating merge diff')
        commonAncestorDiffSource.diffs.each { FieldDiff caSourceFieldDiff ->
            FieldDiff caTargetFieldDiff = commonAncestorDiffTarget.find { it.fieldName == caSourceFieldDiff.fieldName }

            // If diff also exists on the target side then it may be a conflicting change if both sides a different
            // Or it is an identical change in which case it does not need to be included in this merge diff
            if (caTargetFieldDiff) {
                switch (caSourceFieldDiff.diffType) {
                    case ArrayDiff.simpleName:
                        append createArrayMergeDiffPresentOnBothSides(caSourceFieldDiff as ArrayDiff,
                                                                      caTargetFieldDiff as ArrayDiff)
                        break
                    case FieldDiff.simpleName:
                        append createFieldMergeDiffPresentOnBothSides(caSourceFieldDiff, caTargetFieldDiff)
                        break
                    default:
                        log.warn('Unhandled diff type {} on both sides', caSourceFieldDiff.diffType)
                }
            }
            // If no diff between CA and target then this is a non-conflicting change
            else {
                switch (caSourceFieldDiff.diffType) {
                    case ArrayDiff.simpleName:
                        append createArrayMergeDiffPresentOnOneSide(caSourceFieldDiff as ArrayDiff)
                        break
                    case FieldDiff.simpleName:
                        append createFieldMergeDiffPresentOnOneSide(caSourceFieldDiff)
                        break
                    default:
                        log.warn('Unhandled diff type {} on both sides', caSourceFieldDiff.diffType)
                }
            }
        }
        this
    }

    FieldMergeDiff find(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.FieldMergeDiff') Closure closure) {
        diffs.find closure
    }

    static <A extends Diffable> ArrayMergeDiff<A> createArrayMergeDiffPresentOnBothSides(ArrayDiff<A> caSourceArrayDiff,
                                                                                         ArrayDiff<A> caTargetArrayDiff) {
        //        createBaseArrayMergeDiffPresentOnOneSide(caSourceArrayDiff)
        //            .rightHandSide(caTargetArrayDiff.right)
        //            .withCreatedDiffs(createCreationMergeDiffs(caSourceArrayDiff, caTargetArrayDiff))
        //            .withDeletedDiffs(findAllDeletedMergeDiffs(caSourceArrayDiff.deleted, caTargetArrayDiff))
        //        //            .modified(findAllModifiedMergeDiffs(sourceTargetDiff.modified, caSourceDiff, caTargetDiff))
        []
    }


    static <A extends Diffable> ArrayMergeDiff<A> createArrayMergeDiffPresentOnOneSide(ArrayDiff<A> caSourceArrayDiff) {

        // Created and Deleted diffs in this array are left as-is as they are guaranteed to be unique and no issue
        createBaseArrayMergeDiffPresentOnOneSide(caSourceArrayDiff)
            .withCreatedMergeDiffs(createCreationMergeDiffsForOneSide(caSourceArrayDiff.created))
            .withDeletedMergeDiffs(createDeletionMergeDiffsForOneSide(caSourceArrayDiff.deleted))
            .withModifiedMergeDiffs(createModifiedMergeDiffsForOneSide(caSourceArrayDiff.modified))
    }

    static <F> FieldMergeDiff<F> createFieldMergeDiffPresentOnOneSide(FieldDiff<F> caSourceFieldDiff) {
        fieldMergeDiff(caSourceFieldDiff.targetClass)
            .forFieldName(caSourceFieldDiff.fieldName)
            .withSource(caSourceFieldDiff.right)
        // just use whats in the commonAncestor as no caTarget data denotes no difference between the CA and the target
            .withTarget(caSourceFieldDiff.left)
            .withCommonAncestor(caSourceFieldDiff.left) // both diffs have the same LHS
    }

    static <F> FieldMergeDiff<F> createFieldMergeDiffPresentOnBothSides(FieldDiff<F> caSourceFieldDiff, FieldDiff<F> caTargetFieldDiff) {
        createFieldMergeDiffPresentOnOneSide(caSourceFieldDiff)
            .withTarget(caTargetFieldDiff.right)
            .asMergeConflict()
    }

    static <A extends Diffable> ArrayMergeDiff<A> createBaseArrayMergeDiffPresentOnOneSide(ArrayDiff<A> caSourceArrayDiff) {

        // Created and Deleted diffs in this array are left as-is as they are guaranteed to be unique and no issue
        arrayMergeDiff(caSourceArrayDiff.targetClass)
            .forFieldName(caSourceArrayDiff.fieldName)
            .withSource(caSourceArrayDiff.right)
        // just use whats in the commonAncestor as no caTarget data denotes no difference between the CA and the target
            .withTarget(caSourceArrayDiff.left)
            .withCommonAncestor(caSourceArrayDiff.left) // both diffs have the same LHS
    }

    static <M extends Diffable> Collection<MergeDiff<M>> createModifiedMergeDiffsForOneSide(Collection<ObjectDiff<M>> caSourceModifiedDiffs) {
        // Modified diffs represent diffs which have modifications down the chain but no changes on the target side
        // Therefore we can use an empty object diff
        caSourceModifiedDiffs.collect { objDiff ->
            Class<M> targetClass = objDiff.left.class as Class<M>
            mergeDiff(targetClass)
                .forMergingDiffable(objDiff.right)
                .intoDiffable(objDiff.left)
                .havingCommonAncestor(objDiff.left)
                .withCommonAncestorDiffedAgainstSource(objDiff)
                .withCommonAncestorDiffedAgainstTarget(objectDiff(targetClass))
                .generate()
        }
    }

    static <C extends Diffable> Collection<CreationMergeDiff<C>> createCreationMergeDiffsForOneSide(
        Collection<CreationDiff<C>> caSourceCreatedDiffs) {
        caSourceCreatedDiffs.collect { created ->
            creationMergeDiff(created.targetClass)
                .whichCreated(created.created)
        }
    }

    static <D extends Diffable> Collection<DeletionMergeDiff<D>> createDeletionMergeDiffsForOneSide(
        Collection<DeletionDiff<D>> caSourceDeletionDiffs) {
        caSourceDeletionDiffs.collect { deleted ->
            deletionMergeDiff(deleted.targetClass)
                .whichDeleted(deleted.deleted)
        }
    }

    /**
     * Identify all the objects in the array field created on the LHS and flag all those which
     *
     * Important to note that due to the way diff works the same object cannot exist in more than one of created, deleted, modified in an
     * ArrayDIff
     */
    static <C extends Diffable> Collection<CreationMergeDiff> createCreationMergeDiffs(ArrayDiff<C> caSourceDiff,
                                                                                       ArrayDiff<C> caTargetDiff) {
        List<CreationDiff<C>> creationDiffs = caSourceDiff.created.collect { diff ->
            if (diff.createdIdentifier in caTargetDiff.created*.createdIdentifier) {
                // Both sides added : potential conflict and therefore is a modified rather than create or no diff
                log.debug('ca/source created {} exists in ca/target created', diff.createdIdentifier)
                return null
            }
            if (diff.createdIdentifier in caTargetDiff.deleted*.deletedIdentifier) {
                //  Impossible as it didnt exist in CA therefore target can't have deleted it
                return null
            }
            if (diff.createdIdentifier in caTargetDiff.modified*.getRightIdentifier()) {
                //  Impossible as it didnt exist in CA therefore target can't have modified it
                return null
            }
            // Only added on source side : no conflict
            log.debug('ca/source created {} doesnt exist in ca/target', diff.createdIdentifier)
            return creationDiff(diff.targetClass).created(diff.value)
        }.findAll() as Collection<CreationDiff>

        //        creationDiffs.addAll(caSourceDiff.modified.collect { caSourceModifiedDiff ->
        //            DeletionDiff<C> caTargetDeletedDiff = caTargetDiff.deleted.find { it.deletedIdentifier == caSourceModifiedDiff.leftIdentifier }
        //            if (caTargetDeletedDiff) {
        //                // Modified in source but deleted in target : conflict but should appear as creation
        //                return creationDiff(caSourceModifiedDiff.targetClass.arrayType() as Class<C>)
        //                    .created(caSourceModifiedDiff.right)
        //                    .withMergeDeletion(caTargetDeletedDiff.deleted)
        //                    .withCommonAncestor(caSourceDiff.left)
        //                    .asMergeConflict()
        //            }
        //            null
        //        }.findAll() as Collection<CreationDiff<C>>)

        creationDiffs
    }

    /**
     * Identify all the objects in the array field deleted on the LHS and flag all those which
     *
     * Important to note that due to the way diff works the same object cannot exist in more than one of created, deleted, modified in an
     * ArrayDIff
     *
     */
    static <D extends Diffable> Collection<DeletionMergeDiff> findAllDeletedMergeDiffs(Collection<DeletionDiff<D>> caSourceDeletedDiffs,
                                                                                       ArrayDiff<D> caTargetDiff) {
        caSourceDeletedDiffs.collect { diff ->
            if (diff.deletedIdentifier in caTargetDiff.created*.createdIdentifier) {
                //  Impossible as you can't delete something which never existed
                return null
            }
            if (diff.deletedIdentifier in caTargetDiff.deleted*.deletedIdentifier) {
                // Deleted in source, deleted in target : no conflict no diff
                log.debug('ca/source deleted {} exists in ca/target deleted', diff.deletedIdentifier)
                return null
            }
            ObjectDiff<D> modifiedTargetDiff = caTargetDiff.modified.find { it.rightIdentifier == diff.deletedIdentifier }
            if (modifiedTargetDiff) {
                // Deleted in source, modified in target : conflict as deletion diff
                log.debug('ca/source deleted {} exists in ca/target modified', diff.deletedIdentifier)
                return deletionDiff(diff.targetClass)
                    .deleted(diff.value)
                    .withMergeModification(modifiedTargetDiff.right)
                    .withCommonAncestor(modifiedTargetDiff.left)
                    .asMergeConflict()
            }
            // Deleted in source but not touched in target : no conflict
            log.debug('ca/source deleted {} doesnt exist in ca/target', diff.deletedIdentifier)
            return deletionDiff(diff.targetClass).deleted(diff.value).withNoMergeModification()
        }.findAll() as Collection<DeletionDiff>
    }

    //    static Collection<ObjectDiff> findAllModifiedMergeDiffs(Collection<ObjectDiff> modified, ArrayDiff sourceArrayDiff, ArrayDiff
    //    targetArrayDiff) {
    //        modified.collect { ObjectDiff objDiff ->
    //            def diffIdentifier = objDiff.target.diffIdentifier
    //            if (diffIdentifier in sourceArrayDiff.created.value.diffIdentifier) { return updateModifiedObjectMergeDiffCreatedOnOneSide
    //            (objDiff) }
    //            if
    //            (diffIdentifier in sourceArrayDiff.modified.source.diffIdentifier) {
    //                if (diffIdentifier in targetArrayDiff.modified.source.diffIdentifier) {
    //                    //
    //                    top modified, source modified , target modified
    //                    return createModifiedObjectMergeDiffModifiedOnBothSides(objDiff,
    //                                                                            sourceArrayDiff.modified.
    //                                                                                find { it.source.diffIdentifier == diffIdentifier } as
    //                                                                                ObjectDiff,
    //                                                                            targetArrayDiff.modified.
    //                                                                                find { it.source.diffIdentifier == diffIdentifier } as
    //                                                                                ObjectDiff,
    //                                                                            targetArrayDiff.source.find { it.diffIdentifier ==
    //                                                                            diffIdentifier })
    //                } // top modified, source modified, target not modified
    //                return updateModifiedObjectMergeDiffPresentOnOneSide(objDiff)
    //            } null
    //        }.findAll()
    //    }
    //    static ObjectDiff updateModifiedObjectMergeDiffCreatedOnOneSide(ObjectDiff objectDiff) {
    //        // top modified, target created, (source also created)
    //        objectDiff.diffs.each {
    //            it.isMergeConflict = true
    //            it.commonAncestorValue = null
    //        } objectDiff . isMergeConflict = true
    //        objectDiff.commonAncestorValue = null
    //        return objectDiff
    //    }
    //
    //    static ObjectDiff createModifiedObjectMergeDiffModifiedOnBothSides(ObjectDiff objectDiff, ObjectDiff sourceObjDiff,
    //                                                                       ObjectDiff targetObjDiff,
    //                                                                       Object commonAncestorValue) {
    //        // call recursively
    //        ObjectDiff mergeDiff = objectDiff.mergeDiff(sourceObjDiff, targetObjDiff)
    //        mergeDiff.isMergeConflict = true
    //        mergeDiff.commonAncestorValue = commonAncestorValue
    //        return mergeDiff
    //    }
    //
    //    static ObjectDiff updateModifiedObjectMergeDiffPresentOnOneSide(ObjectDiff objectDiff) {
    //        objectDiff.diffs.each {
    //            it.isMergeConflict = false
    //        }
    //        objectDiff.isMergeConflict = false
    //        objectDiff
    //    }
}
