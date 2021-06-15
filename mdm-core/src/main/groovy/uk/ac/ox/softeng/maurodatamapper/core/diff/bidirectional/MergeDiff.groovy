package uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional


import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import static uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff.isArrayDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff.isFieldDiff

class MergeDiff<M extends Diffable> extends ObjectDiff<M> {

    MergeDiff(Class<M> targetClass) {
        super(targetClass)
    }


    /**
     * Filters an ObjectDiff of two {@code Diffable}s based on the differences of each of the Diffables to their common ancestor. See MC-9228
     * for details on filtering criteria.
     * @param leftMergeDiff ObjectDiff between left Diffable and commonAncestor Diffable
     * @param rightMergeDiff ObjectDiff between right Diffable and commonAncestor Diffable
     * @return this ObjectDiff with f
     */
    @SuppressWarnings('GroovyVariableNotAssigned')
    ObjectDiff<M> mergeDiff(ObjectDiff<M> leftMergeDiff, ObjectDiff<M> rightMergeDiff) {
        List<FieldDiff> existingDiffs = new ArrayList<>(this.diffs)

        List<String> leftMergeDiffFieldNames = leftMergeDiff.diffs.fieldName
        List<String> rightMergeDiffFieldNames = rightMergeDiff.diffs.fieldName

        this.diffs = existingDiffs.collect { diff ->

            if (diff.fieldName in leftMergeDiffFieldNames && diff.fieldName in rightMergeDiffFieldNames) {
                if (isArrayDiff(diff)) {
                    return updateArrayMergeDiffPresentOnBothSides(diff as ArrayDiff,
                                                                  leftMergeDiff.diffs.find { it.fieldName == diff.fieldName } as ArrayDiff,
                                                                  rightMergeDiff.diffs.find { it.fieldName == diff.fieldName } as ArrayDiff)
                }
                if (isFieldDiff(diff)) {
                    return updateFieldMergeDiffPresentOnBothSides(diff,
                                                                  rightMergeDiff.diffs.find { it.fieldName == diff.fieldName })
                }
            }

            // An entire Diffable type can not be present on the right, so there will be no merge conflicts
            if (diff.fieldName in leftMergeDiffFieldNames) {
                if (isArrayDiff(diff)) {
                    return updateArrayMergeDiffPresentOnOneSide(diff as ArrayDiff,
                                                                leftMergeDiff.diffs.find { it.fieldName == diff.fieldName } as ArrayDiff)
                }
                if (isFieldDiff(diff)) {
                    return updateFieldMergeDiffPresentOnOneSide(diff as FieldDiff)
                }
            }
            // If not in LHS then dont add
            null
        }.findAll() // Strip null values
        this
    }

    ArrayDiff updateArrayMergeDiffPresentOnBothSides(ArrayDiff arrayDiff, ArrayDiff leftArrayDiff, ArrayDiff rightArrayDiff) {

        arrayDiff.created = findAllCreatedMergeDiffs(arrayDiff.created, leftArrayDiff)
        arrayDiff.deleted = findAllDeletedMergeDiffs(arrayDiff.deleted, leftArrayDiff, rightArrayDiff)
        arrayDiff.modified = findAllModifiedMergeDiffs(arrayDiff.modified, leftArrayDiff, rightArrayDiff)
        arrayDiff
    }

    FieldDiff updateFieldMergeDiffPresentOnBothSides(FieldDiff diff, FieldDiff rightFieldDiff) {
        diff.isMergeConflict = true
        diff.commonAncestorValue = rightFieldDiff.left
        diff
    }

    ArrayDiff updateArrayMergeDiffPresentOnOneSide(ArrayDiff arrayDiff, ArrayDiff leftArrayDiff) {
        arrayDiff.deleted.each { it.isMergeConflict = false }
        arrayDiff.created.each { it.isMergeConflict = false }

        arrayDiff.modified.each { objDiff ->
            def diffIdentifier = objDiff.right.diffIdentifier
            def leftObjDiff = leftArrayDiff.modified.find { it.left.diffIdentifier == diffIdentifier } as ObjectDiff
            // call recursively
            objDiff = objDiff.mergeDiff(leftObjDiff, new ObjectDiff<>())
            objDiff.isMergeConflict = false
        }
        arrayDiff
    }

    FieldDiff updateFieldMergeDiffPresentOnOneSide(FieldDiff fieldDiff) {
        fieldDiff.isMergeConflict = false
        fieldDiff
    }

    Collection<MergeWrapper> findAllCreatedMergeDiffs(Collection<MergeWrapper> created, ArrayDiff leftArrayDiff) {
        created.collect { MergeWrapper wrapper ->
            def diffIdentifier = wrapper.value.diffIdentifier
            if (diffIdentifier in leftArrayDiff.created.value.diffIdentifier) {
                // top created, left created
                wrapper.isMergeConflict = false
                return wrapper
            }
            if (diffIdentifier in leftArrayDiff.modified.left.diffIdentifier) {
                // top created, left modified
                wrapper.isMergeConflict = true
                wrapper.commonAncestorValue = leftArrayDiff.left.find { it.diffIdentifier == diffIdentifier }
                return wrapper
            }
            null
        }.findAll()
    }

    Collection<MergeWrapper> findAllDeletedMergeDiffs(Collection<MergeWrapper> deleted, ArrayDiff leftArrayDiff, ArrayDiff rightArrayDiff) {
        deleted.collect { MergeWrapper wrapper ->
            def diffIdentifier = wrapper.value.diffIdentifier
            if (diffIdentifier in rightArrayDiff.modified.left.diffIdentifier) {
                // top deleted, right modified
                wrapper.isMergeConflict = true
                wrapper.commonAncestorValue = rightArrayDiff.left.find { it.diffIdentifier == diffIdentifier }
                return wrapper
            } else if (diffIdentifier in leftArrayDiff.deleted.value.diffIdentifier) {
                // top deleted, right not modified, left deleted
                wrapper.isMergeConflict = false
                return wrapper
            }
            null
        }.findAll()
    }

    Collection<ObjectDiff> findAllModifiedMergeDiffs(Collection<ObjectDiff> modified, ArrayDiff leftArrayDiff, ArrayDiff rightArrayDiff) {
        modified.collect { ObjectDiff objDiff ->
            def diffIdentifier = objDiff.right.diffIdentifier
            if (diffIdentifier in leftArrayDiff.created.value.diffIdentifier) {
                return updateModifiedObjectMergeDiffCreatedOnOneSide(objDiff)
            }

            if (diffIdentifier in leftArrayDiff.modified.left.diffIdentifier) {
                if (diffIdentifier in rightArrayDiff.modified.left.diffIdentifier) {
                    // top modified, left modified, right modified
                    return createModifiedObjectMergeDiffModifiedOnBothSides(objDiff,
                                                                            leftArrayDiff.modified.
                                                                                find { it.left.diffIdentifier == diffIdentifier } as ObjectDiff,
                                                                            rightArrayDiff.modified.
                                                                                find { it.left.diffIdentifier == diffIdentifier } as ObjectDiff,
                                                                            rightArrayDiff.left.find { it.diffIdentifier == diffIdentifier }
                    )
                }
                // top modified, left modified, right not modified
                return updateModifiedObjectMergeDiffPresentOnOneSide(objDiff)
            }
            null
        }.findAll()
    }

    ObjectDiff updateModifiedObjectMergeDiffCreatedOnOneSide(ObjectDiff objectDiff) {
        // top modified, right created, (left also created)
        objectDiff.diffs.each {
            it.isMergeConflict = true
            it.commonAncestorValue = null
        }
        objectDiff.isMergeConflict = true
        objectDiff.commonAncestorValue = null
        return objectDiff
    }

    ObjectDiff createModifiedObjectMergeDiffModifiedOnBothSides(ObjectDiff objectDiff, ObjectDiff leftObjDiff, ObjectDiff rightObjDiff,
                                                                Object commonAncestorValue) {
        // call recursively
        ObjectDiff mergeDiff = objectDiff.mergeDiff(leftObjDiff, rightObjDiff)
        mergeDiff.isMergeConflict = true
        mergeDiff.commonAncestorValue = commonAncestorValue
        return mergeDiff
    }

    ObjectDiff updateModifiedObjectMergeDiffPresentOnOneSide(ObjectDiff objectDiff) {
        objectDiff.diffs.each { it.isMergeConflict = false }
        objectDiff.isMergeConflict = false
        objectDiff
    }
}
