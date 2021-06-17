package uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional


import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.CreationDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.DeletionDiff

import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.arrayDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.creationDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.deletionDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.fieldDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.mergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff.isArrayDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff.isFieldDiff

/**
 * Holds the filtered result of 3 diffs which provides a unified diff of the changes which one object has made which another object has not made
 * based off a common ancestor
 *
 * The final diff should be the intent of the changes to merge the LHS/source INTO the LHS/target
 *
 * <pre>
 *  LHS --- sourceTargetObjectDiff / top --->  RHS
 *    ^                                     ^
 *     \                                   /
 *   caSourceObjectDiff / source     caTargetObjectDiff / target
 *           \                           /
 *            \                         /
 *                  commonAncestor
 * </pre>
 *
 *
 */
@Slf4j
class MergeDiff<M extends Diffable> extends ObjectDiff<M> {

    MergeDiff(Class<M> targetClass) {
        super(targetClass)
    }

    MergeDiff<M> leftHandSide(M lhs) {
        super.leftHandSide(lhs) as MergeDiff<M>
    }

    MergeDiff<M> rightHandSide(M rhs) {
        super.rightHandSide(rhs) as MergeDiff<M>
    }

    MergeDiff<M> commonAncestor(M ca) {
        super.commonAncestor(ca) as MergeDiff<M>
    }

    /**
     * Filters an ObjectDiff of two {@code Diffable}s based on the differences of each of the Diffables to their common ancestor. See MC-9228
     * for details on filtering criteria.
     *
     * The resulting MergeDiff should display all the actual differences including merge conflicts with the intent of merging the LHS/source into the
     * RHS/target.
     *
     * @param sourceTargetObjectDiff Object diff between LHS and RHS
     * @param caSourceObjectDiff ObjectDiff between common ancestor and LHS
     * @param caTargetObjectDiff ObjectDiff between common ancestor and RHS
     * @return this ObjectDiff with f
     */
    @SuppressWarnings('GroovyVariableNotAssigned')
    MergeDiff<M> diff(ObjectDiff<M> sourceTargetObjectDiff, ObjectDiff<M> caSourceObjectDiff, ObjectDiff<M> caTargetObjectDiff) {

        this.diffs = sourceTargetObjectDiff.diffs.collect { sourceTargetDiff ->

            String diffFieldName = sourceTargetDiff.fieldName
            FieldDiff caSourceFieldDiff = caSourceObjectDiff.find { it.fieldName == diffFieldName }
            FieldDiff caTargetFieldDiff = caTargetObjectDiff.find { it.fieldName == diffFieldName }
            /*
             * Fieldname of a diff between the source and target will be either
             *  - a change between caSource AND caTarget : both sides made a change likely resulting in a merge conflict as there is now a diff
             *  - a change between caSource ONLY : only change is on the source side no change on the target side
             *
             * Because fields are hierarchical a diff on both sides does not indicate a definite merge conflict on arrays
             */

            // If the fieldname is in the caSource and caTarget then there are changes to the field on both sides from the CA
            // and those changes differ which should result in a merge conflict
            if (caSourceFieldDiff && caTargetFieldDiff) {
                if (isArrayDiff(sourceTargetDiff)) {
                    return createArrayMergeDiffPresentOnBothSides(sourceTargetDiff as ArrayDiff,
                                                                  caSourceFieldDiff as ArrayDiff,
                                                                  caTargetFieldDiff as ArrayDiff)
                }
                if (isFieldDiff(sourceTargetDiff)) {
                    return createFieldMergeDiffPresentOnBothSides(sourceTargetDiff, caSourceFieldDiff.left)
                }
                log.warn('Unhandled diff type {}', sourceTargetDiff.diffType)
                return null
            }

            // If the field name is not in the caTarget then it is a diff added from the source side only
            if (caSourceFieldDiff) {
                if (isArrayDiff(sourceTargetDiff)) {
                    return createArrayMergeDiffPresentOnOneSide(sourceTargetDiff as ArrayDiff,
                                                                caSourceFieldDiff as ArrayDiff)
                }
                if (isFieldDiff(sourceTargetDiff)) {
                    return createFieldMergeDiffPresentOnOneSide(sourceTargetDiff, caSourceFieldDiff.left)
                }
                log.warn('Unhandled diff type {}', sourceTargetDiff.diffType)
                return null
            }
            // If the field name is not in the source side then its not relevant wrt merging from target to source
            null
        }.findAll() // Strip null values
        this
    }

    static <A extends Diffable> ArrayDiff<A> createArrayMergeDiffPresentOnBothSides(ArrayDiff<A> sourceTargetDiff,
                                                                                    ArrayDiff<A> caSourceDiff,
                                                                                    ArrayDiff<A> caTargetDiff) {
        arrayDiff(sourceTargetDiff)
            .commonAncestor(caSourceDiff.left)
            .withCreatedDiffs(createCreationMergeDiffs(sourceTargetDiff.created, caSourceDiff, caTargetDiff))
            .withDeletedDiffs(findAllDeletedMergeDiffs(sourceTargetDiff.deleted, caSourceDiff, caTargetDiff))
        //            .modified(findAllModifiedMergeDiffs(sourceTargetDiff.modified, caSourceDiff, caTargetDiff)) as ArrayDiff<A>
    }


    static <A extends Diffable> ArrayDiff<A> createArrayMergeDiffPresentOnOneSide(ArrayDiff<A> sourceTargetDiff, ArrayDiff<A> caSourceDiff) {

        // Modified diffs represent diffs which have modifications down the chain
        Collection<MergeDiff> modifiedMergeDiffs = sourceTargetDiff.modified.collect { objDiff ->
            // Identify the relevant caSourceObjectDiff for this modified diff
            ObjectDiff caSourceObjectDiff = caSourceDiff.modified.find { it.rightIdentifier == objDiff.leftIdentifier }
            // call recursively with no diffs on the caTarget side
            mergeDiff(objDiff, caSourceObjectDiff.right).diff(objDiff, caSourceObjectDiff, new ObjectDiff<>())
        }

        // Created and Deleted diffs in this array are left as-is as they are guaranteed to be unique and no issue
        arrayDiff(sourceTargetDiff)
            .commonAncestor(caSourceDiff.left)
            .withCreatedDiffs(sourceTargetDiff.created)
            .withDeletedDiffs(sourceTargetDiff.deleted)
            .modified(modifiedMergeDiffs)
    }

    static <F> FieldDiff<F> createFieldMergeDiffPresentOnOneSide(FieldDiff<F> sourceTargetDiff, F commonAncestor) {
        fieldDiff(sourceTargetDiff).commonAncestor(commonAncestor)
    }

    static <F> FieldDiff<F> createFieldMergeDiffPresentOnBothSides(FieldDiff<F> sourceTargetDiff, F commonAncestor) {
        createFieldMergeDiffPresentOnOneSide(sourceTargetDiff, commonAncestor).asMergeConflict()
    }

    /**
     * Identify all the objects in the array field created on the LHS and flag all those which
     *
     * Important to note that due to the way diff works the same object cannot exist in more than one of created, deleted, modified in an ArrayDIff
     */
    static <C extends Diffable> Collection<CreationDiff> createCreationMergeDiffs(Collection<CreationDiff<C>> created,
                                                                                  ArrayDiff<C> caSourceDiff,
                                                                                  ArrayDiff<C> caTargetDiff) {
        created.collect { diff ->
            // top created, source created, target doesnt exist
            if (diff.valueIdentifier in caSourceDiff.created*.valueIdentifier) {
                return creationDiff(diff.targetClass).created(diff.value)
            }
            if (diff.valueIdentifier in caSourceDiff.deleted*.valueIdentifier) {
                log.info('source/target created {} exists in ca/source deleted', diff.valueIdentifier)
            }
            if (diff.valueIdentifier in caSourceDiff.modified*.getRightIdentifier()) {
                log.info('source/target created {} exists in ca/source modified', diff.valueIdentifier)
            }
            if (diff.valueIdentifier in caTargetDiff.created*.valueIdentifier) {
                log.info('source/target created {} exists in ca/target created', diff.valueIdentifier)
            }
            if (diff.valueIdentifier in caTargetDiff.deleted*.valueIdentifier) {
                log.info('source/target created {} exists in ca/target created', diff.valueIdentifier)
            }
            if (diff.valueIdentifier in caTargetDiff.modified*.getRightIdentifier()) {
                log.info('source/target created {} exists in ca/target modified', diff.valueIdentifier)
            }
            null
            //            if (diff.valueIdentifier in caSourceDiff.modified*.sourceIdentifier) {
            //                // top created, source modified
            //                return creationDiff(diff.targetClass)
            //                    .created(diff.value)
            //                    .commonAncestor(caSourceDiff.source.find { it.diffIdentifier == diff.valueIdentifier })
            //                    .asMergeConflict()
            //            } null
        }.findAll() as Collection<CreationDiff>
    }

    /**
     * Identify all the objects in the array field deleted on the LHS and flag all those which
     *
     * Important to note that due to the way diff works the same object cannot exist in more than one of created, deleted, modified in an ArrayDIff
     *
     */
    static <D extends Diffable> Collection<DeletionDiff> findAllDeletedMergeDiffs(Collection<DeletionDiff<D>> deleted,
                                                                                  ArrayDiff<D> caSourceDiff,
                                                                                  ArrayDiff<D> caTargetDiff) {
        deleted.collect { diff ->

            if (diff.valueIdentifier in caSourceDiff.created*.valueIdentifier) {
                log.info('source/target deleted {} exists in ca/source created', diff.valueIdentifier)
            }
            // top deleted, source deleted, target not modified
            if (diff.valueIdentifier in caSourceDiff.deleted*.valueIdentifier) {
                return deletionDiff(diff.targetClass).deleted(diff.value)
            }
            if (diff.valueIdentifier in caSourceDiff.modified*.getRightIdentifier()) {
                log.info('source/target deleted {} exists in ca/source modified', diff.valueIdentifier)
            }
            if (diff.valueIdentifier in caTargetDiff.created*.valueIdentifier) {
                log.info('source/target deleted {} exists in ca/target created', diff.valueIdentifier)
            }
            if (diff.valueIdentifier in caTargetDiff.deleted*.valueIdentifier) {
                log.info('source/target deleted {} exists in ca/target created', diff.valueIdentifier)
            }
            if (diff.valueIdentifier in caTargetDiff.modified*.getRightIdentifier()) {
                log.info('source/target deleted {} exists in ca/target modified', diff.valueIdentifier)
            }
            //            if (diff.valueIdentifier in caTargetDiff.modified.source.diffIdentifier) {
            //                // top deleted, target modified
            //                return deletionDiff(diff.targetClass)
            //                    .deleted(diff.value)
            //                    .commonAncestor(caTargetDiff.source.find { it.diffIdentifier == diff.valueIdentifier })
            //                    .asMergeConflict()
            //            }
            null
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
    //                                                                            targetArrayDiff.source.find { it.diffIdentifier == diffIdentifier })
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
