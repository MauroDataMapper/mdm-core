/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional


import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.CreationDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.DeletionDiff
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Path

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.arrayMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.creationMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.deletionMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.fieldMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.mergeDiff

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
@CompileStatic
class MergeDiff<M extends Diffable> extends TriDirectionalDiff<M> implements Comparable<MergeDiff> {

    private List<FieldMergeDiff> diffs

    private ObjectDiff<M> commonAncestorDiffSource
    private ObjectDiff<M> commonAncestorDiffTarget
    private ObjectDiff<M> sourceDiffTarget

    MergeDiff(Class<M> targetClass) {
        super(targetClass)
        diffs = []
    }

    @Override
    Boolean isMergeConflict() {
        !objectsAreIdentical()
    }

    @Override
    Integer getNumberOfDiffs() {
        diffs?.sum {it.getNumberOfDiffs()} as Integer ?: 0
    }

    String getSourceIdentifier() {
        source.diffIdentifier
    }

    String getTargetIdentifier() {
        target.diffIdentifier
    }

    String getSourceId() {
        (source as CreatorAware).id
    }

    String getTargetId() {
        (target as CreatorAware).id
    }

    Path getFullyQualifiedPath() {
        String cleanedIdentifier = sourceIdentifier.split('/').last()
        Path.from(fullyQualifiedObjectPath, source.pathPrefix, cleanedIdentifier)
    }

    FieldMergeDiff first() {
        diffs.first()
    }

    int size() {
        diffs.size()
    }

    boolean isEmpty() {
        diffs.isEmpty()
    }

    List<FieldMergeDiff> getDiffs() {
        diffs.sort()
    }

    List<TriDirectionalDiff> getFlattenedDiffs() {
        diffs.sort().collectMany { diff ->
            if (diff.diffType == FieldMergeDiff.simpleName) return [diff]
            if (diff.diffType == ArrayMergeDiff.simpleName) {
                return (diff as ArrayMergeDiff).getFlattenedDiffs()
            }
            []
        } as List<TriDirectionalDiff>
    }

    @Override
    String toString() {
        String str = "${sourceIdentifier} --> ${targetIdentifier}"
        if (commonAncestor) str = "${str} [${commonAncestor.diffIdentifier}]"
        str
    }

    MergeDiff<M> forMergingDiffable(M sourceSide) {
        super.withSource(sourceSide) as MergeDiff<M>
    }

    @Override
    MergeDiff<M> insideFullyQualifiedObjectPath(Path fullyQualifiedObjectPath) {
        super.insideFullyQualifiedObjectPath(fullyQualifiedObjectPath) as MergeDiff<M>
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

    MergeDiff<M> withSourceDiffedAgainstTarget(ObjectDiff<M> sourceDiffTarget) {
        this.sourceDiffTarget = sourceDiffTarget
        this
    }

    MergeDiff<M> append(FieldMergeDiff fieldDiff) {
        if (fieldDiff) diffs.add(fieldDiff)
        this
    }

    MergeDiff<M> asMergeConflict() {
        super.asMergeConflict() as MergeDiff<M>
    }

    FieldMergeDiff find(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.FieldMergeDiff') Closure closure) {
        diffs.find closure
    }

    @Override
    int compareTo(MergeDiff that) {
        this.sourceIdentifier <=> that.sourceIdentifier
    }

    /**
     * The resulting MergeDiff should display all the actual differences including merge conflicts with the intent of merging the LHS/source into the
     * RHS/target.
     *
     */
    @SuppressWarnings('GroovyVariableNotAssigned')
    MergeDiff<M> generate() {

        // Both sides then calculate the merge
        if (commonAncestorDiffSource && commonAncestorDiffTarget) {
            return generateMergeDiffFromCommonAncestorAgainstSourceAndTarget()
        }
        // Only source means this is a merge diff recurse where there is no content on the target side
        if (commonAncestorDiffSource) {
            return generateMergeDiffFromCommonAncestorAgainstSource()
        }
        // Otherwise we have the source diff target where there is no common ancestor
        generateMergeDiffFromSourceAgainstTarget()
    }

    private MergeDiff<M> generateMergeDiffFromCommonAncestorAgainstSourceAndTarget() {

        log.debug('Generating merge diff from common ancestor diffs against source and target for [{}]', commonAncestorDiffSource.leftIdentifier)
        commonAncestorDiffSource.diffs.each {FieldDiff caSourceFieldDiff ->

            log.debug('Processing field [{}] with change type [{}] present between common ancestor and source', caSourceFieldDiff.fieldName, caSourceFieldDiff.diffType)
            FieldDiff caTargetFieldDiff = commonAncestorDiffTarget.find {it.fieldName == caSourceFieldDiff.fieldName}

            // If diff also exists on the target side then it may be a conflicting change if both sides a different
            // Or it is an identical change in which case it does not need to be included in this merge diff
            if (caTargetFieldDiff) {
                log.debug('[{}] Change present between common ancestor and target', caSourceFieldDiff.fieldName)
                switch (caSourceFieldDiff.diffType) {
                    case ArrayDiff.simpleName:
                        append createArrayMergeDiffPresentOnBothSides(fullyQualifiedPath,
                                                                      caSourceFieldDiff as ArrayDiff,
                                                                      caTargetFieldDiff as ArrayDiff)
                        break
                    case FieldDiff.simpleName:
                        append createFieldMergeDiffPresentOnBothSides(fullyQualifiedPath, caSourceFieldDiff, caTargetFieldDiff)
                        break
                    default:
                        log.warn('Unhandled diff type {} on both sides', caSourceFieldDiff.diffType)
                }
            }
            // If no diff between CA and target then this is a non-conflicting change
            else {
                log.debug('[{}] No change present between common ancestor and target', caSourceFieldDiff.fieldName)
                switch (caSourceFieldDiff.diffType) {
                    case ArrayDiff.simpleName:
                        append createArrayMergeDiffPresentOnOneSide(fullyQualifiedPath, caSourceFieldDiff as ArrayDiff)
                        break
                    case FieldDiff.simpleName:
                        append createFieldMergeDiffPresentOnOneSide(fullyQualifiedPath, caSourceFieldDiff)
                        break
                    default:
                        log.warn('Unhandled diff type {} on both sides', caSourceFieldDiff.diffType)
                }
            }
        }
        this
    }

    private MergeDiff<M> generateMergeDiffFromCommonAncestorAgainstSource() {

        log.debug('Generating merge diff from common ancestor diff against source for [{}]', commonAncestorDiffSource.leftIdentifier)
        commonAncestorDiffSource.diffs.each {FieldDiff caSourceFieldDiff ->
            log.debug('Processing field [{}] with change type [{}] present between common ancestor and source', caSourceFieldDiff.fieldName, caSourceFieldDiff.diffType)
            switch (caSourceFieldDiff.diffType) {
                case ArrayDiff.simpleName:
                    append createArrayMergeDiffPresentOnOneSide(fullyQualifiedPath, caSourceFieldDiff as ArrayDiff)
                    break
                case FieldDiff.simpleName:
                    append createFieldMergeDiffPresentOnOneSide(fullyQualifiedPath, caSourceFieldDiff)
                    break
                default:
                    log.warn('Unhandled diff type {} on both sides', caSourceFieldDiff.diffType)
            }
        }
        this
    }

    private MergeDiff<M> generateMergeDiffFromSourceAgainstTarget() {
        log.debug('Generating merge diff from source diff against target for [{}]', sourceDiffTarget.leftIdentifier)

        sourceDiffTarget.diffs.each {FieldDiff sourceTargetFieldDiff ->
            log.debug('Processing field [{}] with change type [{}] present between source and target', sourceTargetFieldDiff.fieldName, sourceTargetFieldDiff.diffType)
            switch (sourceTargetFieldDiff.diffType) {
                case ArrayDiff.simpleName:
                    append createArrayMergeDiffFromSourceTargetArrayDiff(fullyQualifiedPath, sourceTargetFieldDiff as ArrayDiff)
                    break
                case FieldDiff.simpleName:
                    append createFieldMergeDiffFromSourceTargetFieldDiff(fullyQualifiedPath, sourceTargetFieldDiff)
                    break
                default:
                    log.warn('Unhandled diff type {} on both sides', sourceTargetFieldDiff.diffType)
            }
        }
        this
    }

    static <A extends Diffable> ArrayMergeDiff<A> createArrayMergeDiffFromSourceTargetArrayDiff(Path fullyQualifiedObjectPath, ArrayDiff<A> sourceTargetArrayDiff) {
        log.debug('[{}] Processing array differences against target from source', sourceTargetArrayDiff.fieldName)
        // Created and Deleted diffs in this array are left as-is as they are guaranteed to be unique and no issue
        arrayMergeDiff(sourceTargetArrayDiff.targetClass)
            .forFieldName(sourceTargetArrayDiff.fieldName)
            .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            .withSource(sourceTargetArrayDiff.left)
            .withTarget(sourceTargetArrayDiff.right)
            .withCommonAncestor(null)
            .withCreatedMergeDiffs(sourceTargetArrayDiff.created.collect {c ->
                creationMergeDiff(c.targetClass)
                    .whichCreated(c.created)
                    .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            })
            .withDeletedMergeDiffs(sourceTargetArrayDiff.deleted.collect {d ->
                deletionMergeDiff(d.targetClass)
                    .whichDeleted(d.deleted)
                    .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            })
            .withModifiedMergeDiffs(sourceTargetArrayDiff.modified.collect {m ->
                mergeDiff(m.targetClass)
                    .forMergingDiffable(m.left)
                    .intoDiffable(m.right)
                    .havingCommonAncestor(null)
                    .withSourceDiffedAgainstTarget(m)
                    .generate()
            })

    }

    static <A extends Diffable> ArrayMergeDiff<A> createBaseArrayMergeDiffPresentOnOneSide(Path fullyQualifiedObjectPath,
                                                                                           ArrayDiff<A> caSourceArrayDiff) {

        // Created and Deleted diffs in this array are left as-is as they are guaranteed to be unique and no issue
        arrayMergeDiff(caSourceArrayDiff.targetClass)
            .forFieldName(caSourceArrayDiff.fieldName)
            .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            .withSource(caSourceArrayDiff.right)
        // just use whats in the commonAncestor as no caTarget data denotes no difference between the CA and the target
            .withTarget(caSourceArrayDiff.left)
            .withCommonAncestor(caSourceArrayDiff.left) // both diffs have the same LHS
    }

    static <A extends Diffable> ArrayMergeDiff<A> createArrayMergeDiffPresentOnBothSides(Path fullyQualifiedObjectPath,
                                                                                         ArrayDiff<A> caSourceArrayDiff,
                                                                                         ArrayDiff<A> caTargetArrayDiff) {
        log.debug('[{}] Processing array differences against common ancestor on both sides', caSourceArrayDiff.fieldName)
        createBaseArrayMergeDiffPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff)
            .withTarget(caTargetArrayDiff.right)
            .withCreatedMergeDiffs(createCreationMergeDiffsPresentOnBothSides(fullyQualifiedObjectPath, caSourceArrayDiff, caTargetArrayDiff))
            .withDeletedMergeDiffs(createDeletionMergeDiffsPresentOnBothSides(fullyQualifiedObjectPath, caSourceArrayDiff.deleted, caTargetArrayDiff))
            .withModifiedMergeDiffs(createModifiedMergeDiffsPresentOnBothSides(fullyQualifiedObjectPath, caSourceArrayDiff, caTargetArrayDiff))
            .getValidOnly()
    }


    static <A extends Diffable> ArrayMergeDiff<A> createArrayMergeDiffPresentOnOneSide(Path fullyQualifiedObjectPath,
                                                                                       ArrayDiff<A> caSourceArrayDiff) {
        log.debug('[{}] Processing array differences against common ancestor on one side', caSourceArrayDiff.fieldName)
        // Created and Deleted diffs in this array are left as-is as they are guaranteed to be unique and no issue
        createBaseArrayMergeDiffPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff)
            .withCreatedMergeDiffs(createCreationMergeDiffsPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff.created))
            .withDeletedMergeDiffs(createDeletionMergeDiffsPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff.deleted))
            .withModifiedMergeDiffs(createModifiedMergeDiffsPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff.modified))
            .getValidOnly()
    }

    static <F> FieldMergeDiff<F> createFieldMergeDiffFromSourceTargetFieldDiff(Path fullyQualifiedObjectPath, FieldDiff<F> sourceTargetFieldDiff) {
        log.debug('[{}] Processing field difference against target from source', sourceTargetFieldDiff.fieldName)
        fieldMergeDiff(sourceTargetFieldDiff.targetClass)
            .forFieldName(sourceTargetFieldDiff.fieldName)
            .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            .withSource(sourceTargetFieldDiff.left)
            .withTarget(sourceTargetFieldDiff.right)
            .withCommonAncestor(null)
            .asMergeConflict() // Always a merge conflict as the values have to be different otherwise we wouldnt be here
            .getValidOnly()
    }

    static <F> FieldMergeDiff<F> createFieldMergeDiffPresentOnBothSides(Path fullyQualifiedObjectPath, FieldDiff<F> caSourceFieldDiff, FieldDiff<F> caTargetFieldDiff) {
        log.debug('[{}] Processing field difference against common ancestor on both sides', caSourceFieldDiff.fieldName)
        createBaseFieldMergeDiffPresentOnOneSide(fullyQualifiedObjectPath, caSourceFieldDiff)
            .withTarget(caTargetFieldDiff.right)
            .asMergeConflict()
            .getValidOnly() // This is a safety check to handle when 2 diffs are used with modifications but no actual difference
    }

    static <F> FieldMergeDiff<F> createFieldMergeDiffPresentOnOneSide(Path fullyQualifiedObjectPath, FieldDiff<F> caSourceFieldDiff) {
        log.debug('[{}] Processing field difference against common ancestor on one side', caSourceFieldDiff.fieldName)
        createBaseFieldMergeDiffPresentOnOneSide(fullyQualifiedObjectPath, caSourceFieldDiff)
        // just use whats in the commonAncestor as no caTarget data denotes no difference between the CA and the target
            .withTarget(caSourceFieldDiff.left)
            .getValidOnly()
    }

    static <F> FieldMergeDiff<F> createBaseFieldMergeDiffPresentOnOneSide(Path fullyQualifiedObjectPath, FieldDiff<F> caSourceFieldDiff) {
        fieldMergeDiff(caSourceFieldDiff.targetClass)
            .forFieldName(caSourceFieldDiff.fieldName)
            .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            .withSource(caSourceFieldDiff.right)
            .withCommonAncestor(caSourceFieldDiff.left) // both diffs have the same LHS
    }

    static <C extends Diffable> Collection<CreationMergeDiff<C>> createCreationMergeDiffsPresentOnOneSide(Path fullyQualifiedObjectPath,
                                                                                                          Collection<CreationDiff<C>> caSourceCreatedDiffs) {
        caSourceCreatedDiffs.collect {created ->
            creationMergeDiff(created.targetClass)
                .whichCreated(created.created)
                .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
        }
    }

    /**
     * Identify all the objects in the array field created on the LHS and flag in the logs all those which were created on both sides as they may be a merge conflict
     *
     * Important to note that due to the way diff works the same object cannot exist in more than one of created, deleted, modified in an
     * ArrayDIff
     */
    static <C extends Diffable> Collection<CreationMergeDiff> createCreationMergeDiffsPresentOnBothSides(Path fullyQualifiedObjectPath,
                                                                                                         ArrayDiff<C> caSourceDiff,
                                                                                                         ArrayDiff<C> caTargetDiff) {
        Collection<CreationMergeDiff> modificationCreationMergeDiffs = caSourceDiff.modified.collect {caSourceModificationDiff ->
            DeletionDiff caTargetDeletionDiff = caTargetDiff.deleted.find {it.deletedIdentifier == caSourceModificationDiff.leftIdentifier}
            if (caTargetDeletionDiff) {
                log.debug('[{}] ca/source modified exists in ca/target deleted.', caSourceModificationDiff.leftIdentifier)
                return creationMergeDiff(caSourceModificationDiff.targetClass)
                    .whichCreated(caSourceModificationDiff.right)
                    .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                    .withCommonAncestor(caSourceModificationDiff.left)
                    .asMergeConflict()
            }
            null
        }.findAll()
        Collection<CreationMergeDiff> creationMergeDiffs = caSourceDiff.created.collect {diff ->
            if (diff.createdIdentifier in caTargetDiff.created*.createdIdentifier) {
                // Both sides added : potential conflict and therefore is a modified rather than create or no diff
                log.trace('[{}] ca/source created exists in ca/target created. Possible merge conflict will be rendered as a modified MergeDiff', diff.createdIdentifier)
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
            log.debug('[{}] ca/source created doesnt exist in ca/target', diff.createdIdentifier)
            return creationMergeDiff(diff.targetClass)
                .whichCreated(diff.value)
                .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
        }.findAll()

        creationMergeDiffs.addAll(modificationCreationMergeDiffs)
        creationMergeDiffs
    }


    static <D extends Diffable> Collection<DeletionMergeDiff<D>> createDeletionMergeDiffsPresentOnOneSide(Path fullyQualifiedObjectPath,
                                                                                                          Collection<DeletionDiff<D>> caSourceDeletionDiffs) {
        caSourceDeletionDiffs.collect {deleted ->
            deletionMergeDiff(deleted.targetClass)
                .whichDeleted(deleted.deleted)
                .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
        }
    }

    /**
     * Identify all the objects in the array field deleted on the LHS and flag all those which
     *
     * Important to note that due to the way diff works the same object cannot exist in more than one of created, deleted, modified in an
     * ArrayDIff
     *
     */
    static <D extends Diffable> Collection<DeletionMergeDiff> createDeletionMergeDiffsPresentOnBothSides(Path fullyQualifiedObjectPath,
                                                                                                         Collection<DeletionDiff<D>> caSourceDeletedDiffs,
                                                                                                         ArrayDiff<D> caTargetDiff) {
        caSourceDeletedDiffs.collect {diff ->
            if (diff.deletedIdentifier in caTargetDiff.created*.createdIdentifier) {
                //  Impossible as you can't delete something which never existed
                return null
            }
            if (diff.deletedIdentifier in caTargetDiff.deleted*.deletedIdentifier) {
                // Deleted in source, deleted in target : no conflict no diff
                log.trace('[{}] ca/source deleted exists in ca/target deleted', diff.deletedIdentifier)
                return null
            }
            ObjectDiff<D> caTargetModifiedDiff = caTargetDiff.modified.find {it.rightIdentifier == diff.deletedIdentifier}
            if (caTargetModifiedDiff) {
                // Deleted in source, modified in target : conflict as deletion diff
                log.debug('[{}] ca/source deleted exists in ca/target modified', diff.deletedIdentifier)
                return deletionMergeDiff(diff.targetClass)
                    .whichDeleted(diff.value)
                    .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                    .withMergeModification(caTargetModifiedDiff) // TODO does this work with giving the information needed???
                    .withCommonAncestor(caTargetModifiedDiff.left)
                    .asMergeConflict()
            }
            // Deleted in source but not touched in target : no conflict
            log.debug('[{}] ca/source deleted doesnt exist in ca/target', diff.deletedIdentifier)
            return deletionMergeDiff(diff.targetClass)
                .whichDeleted(diff.value)
                .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                .withNoMergeModification()
        }.findAll()
    }

    static <M extends Diffable> Collection<MergeDiff<M>> createModifiedMergeDiffsPresentOnOneSide(Path fullyQualifiedObjectPath,
                                                                                                  Collection<ObjectDiff<M>> caSourceModifiedDiffs) {
        // Modified diffs represent diffs which have modifications down the chain but no changes on the target side
        // Therefore we can use an empty object diff
        caSourceModifiedDiffs.collect {objDiff ->
            Class<M> targetClass = objDiff.left.class as Class<M>
            mergeDiff(targetClass)
                .forMergingDiffable(objDiff.right)
                .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                .intoDiffable(objDiff.left)
                .havingCommonAncestor(objDiff.left)
                .withCommonAncestorDiffedAgainstSource(objDiff)
                .generate()
        }
    }

    static <M extends Diffable> Collection<MergeDiff<M>> createModifiedMergeDiffsPresentOnBothSides(Path fullyQualifiedObjectPath,
                                                                                                    ArrayDiff<M> caSourceDiff,
                                                                                                    ArrayDiff<M> caTargetDiff) {

        Collection<MergeDiff<M>> modifiedDiffs = caSourceDiff.modified.collect {caSourceModifiedDiff ->
            ObjectDiff<M> caTargetModifiedDiff = caTargetDiff.modified.find {it.leftIdentifier == caSourceModifiedDiff.leftIdentifier}
            if (caTargetModifiedDiff) {
                log.debug('[{}] modified on both sides', caSourceModifiedDiff.leftIdentifier)

                MergeDiff sourceTargetMergeDiff = mergeDiff(caSourceModifiedDiff.targetClass)
                    .forMergingDiffable(caSourceModifiedDiff.right)
                    .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                    .intoDiffable(caTargetModifiedDiff.right)
                    .havingCommonAncestor(caSourceModifiedDiff.left)
                    .withCommonAncestorDiffedAgainstSource(caSourceModifiedDiff)
                    .withCommonAncestorDiffedAgainstTarget(caTargetModifiedDiff)
                    .generate()
                // If no diffs then the modifications are the same so dont include
                return sourceTargetMergeDiff.isEmpty() ? null : sourceTargetMergeDiff

            }
            DeletionDiff<M> caTargetDeletionDiff = caTargetDiff.deleted.find {it.deletedIdentifier == caSourceModifiedDiff.leftIdentifier}
            if (caTargetDeletionDiff) {
                log.debug('[{}] modified on ca/source side and deleted on ca/target side. TREATED AS CREATION', caSourceModifiedDiff.leftIdentifier)
                return null
            }

            log.debug('[{}] only modified on ca/source side', caSourceModifiedDiff.leftIdentifier)
            mergeDiff(caSourceModifiedDiff.targetClass)
                .forMergingDiffable(caSourceModifiedDiff.right)
                .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                .intoDiffable(caSourceModifiedDiff.left)
                .havingCommonAncestor(caSourceModifiedDiff.left)
                .withCommonAncestorDiffedAgainstSource(caSourceModifiedDiff)
                .generate()
        }.findAll()


        Collection<MergeDiff<M>> createdModifiedDiffs = caSourceDiff.created.collect {caSourceCreationDiff ->
            CreationDiff<M> caTargetCreationDiff = caTargetDiff.created.find {it.createdIdentifier == caSourceCreationDiff.createdIdentifier}
            if (caTargetCreationDiff) {
                // Both sides added : potential conflict and therefore is a modified rather than create or no diff
                log.debug('[{}] ca/source created exists in ca/target created. This is a potential merge modification', caSourceCreationDiff.createdIdentifier)

                // Get the diff of the 2 objects, we need to determine if theres actually a merge conflict
                ObjectDiff<M> sourceTargetDiff = caSourceCreationDiff.created.diff(caTargetCreationDiff.created)
                // If objects are identical then theres no merge difference so it can be ignored
                if (sourceTargetDiff.objectsAreIdentical()) {
                    log.debug('Both sides created but the creations are identical')
                    return null
                }

                return mergeDiff(sourceTargetDiff.targetClass as Class<M>)
                    .forMergingDiffable(caSourceCreationDiff.created)
                    .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                    .intoDiffable(caTargetCreationDiff.created)
                    .havingCommonAncestor(null)
                    .withSourceDiffedAgainstTarget(sourceTargetDiff)
                    .generate()
            }
            null
        }.findAll()
        modifiedDiffs.addAll(createdModifiedDiffs)
        modifiedDiffs
    }
}
