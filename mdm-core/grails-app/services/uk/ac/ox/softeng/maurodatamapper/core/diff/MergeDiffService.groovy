/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.diff

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.ArrayMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.CreationMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.DeletionMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.FieldMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.MergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.CreationDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.DeletionDiff
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.path.Path

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.hibernate.LazyInitializationException
import org.springframework.beans.factory.annotation.Autowired

import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.arrayMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.creationMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.deletionMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.fieldMergeDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.mergeDiff

/**
 * @since 21/02/2022
 */
@Transactional
@Slf4j
class MergeDiffService {

    @Autowired(required = false)
    Set<MdmDomainService> mdmDomainServices

    /**
     * The resulting MergeDiff should display all the actual differences including merge conflicts with the intent of merging the LHS/source into the
     * RHS/target.
     *
     */
    @SuppressWarnings('GroovyVariableNotAssigned')
    def <M extends Diffable> MergeDiff<M> generateMergeDiff(MergeDiff<M> mergeDiffToGenerate) {

        // Both sides then calculate the merge
        if (mergeDiffToGenerate.commonAncestorDiffSource && mergeDiffToGenerate.commonAncestorDiffTarget) {
            return generateMergeDiffFromCommonAncestorAgainstSourceAndTarget(mergeDiffToGenerate)
        }
        // Only source means this is a merge diff recurse where there is no content on the target side
        if (mergeDiffToGenerate.commonAncestorDiffSource) {
            return generateMergeDiffFromCommonAncestorAgainstSource(mergeDiffToGenerate)
        }
        // Otherwise we have the source diff target where there is no common ancestor
        generateMergeDiffFromSourceAgainstTarget(mergeDiffToGenerate)
    }

    def <M extends Diffable> MergeDiff<M> generateMergeDiffFromCommonAncestorAgainstSourceAndTarget(MergeDiff<M> mergeDiffToGenerate) {

        log.debug('Generating merge diff from common ancestor diffs against source and target for [{}]', mergeDiffToGenerate.commonAncestorDiffSource.leftIdentifier)
        mergeDiffToGenerate.commonAncestorDiffSource.diffs.each {FieldDiff caSourceFieldDiff ->

            log.debug('Processing field [{}] with change type [{}] present between common ancestor and source', caSourceFieldDiff.fieldName, caSourceFieldDiff.diffType)
            FieldDiff caTargetFieldDiff = mergeDiffToGenerate.commonAncestorDiffTarget.find {it.fieldName == caSourceFieldDiff.fieldName}

            // If diff also exists on the target side then it may be a conflicting change if both sides a different
            // Or it is an identical change in which case it does not need to be included in this merge diff
            if (caTargetFieldDiff) {
                log.debug('[{}] Change present between common ancestor and target', caSourceFieldDiff.fieldName)
                switch (caSourceFieldDiff.diffType) {
                    case ArrayDiff.simpleName:
                        mergeDiffToGenerate.append createArrayMergeDiffPresentOnBothSides(mergeDiffToGenerate.fullyQualifiedPath,
                                                                                          caSourceFieldDiff as ArrayDiff,
                                                                                          caTargetFieldDiff as ArrayDiff,
                                                                                          mergeDiffToGenerate.getSourceDiffCache(),
                                                                                          mergeDiffToGenerate.getTargetDiffCache())
                        break
                    case FieldDiff.simpleName:
                        mergeDiffToGenerate.append createFieldMergeDiffPresentOnBothSides(mergeDiffToGenerate.fullyQualifiedPath, caSourceFieldDiff, caTargetFieldDiff)
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
                        mergeDiffToGenerate.append createArrayMergeDiffPresentOnOneSide(mergeDiffToGenerate.fullyQualifiedPath, caSourceFieldDiff as ArrayDiff)
                        break
                    case FieldDiff.simpleName:
                        mergeDiffToGenerate.append createFieldMergeDiffPresentOnOneSide(mergeDiffToGenerate.fullyQualifiedPath, caSourceFieldDiff)
                        break
                    default:
                        log.warn('Unhandled diff type {} on both sides', caSourceFieldDiff.diffType)
                }
            }
        }
        mergeDiffToGenerate
    }

    def <M extends Diffable> MergeDiff<M> generateMergeDiffFromCommonAncestorAgainstSource(MergeDiff<M> mergeDiffToGenerate) {

        log.debug('Generating merge diff from common ancestor diff against source for [{}]', mergeDiffToGenerate.commonAncestorDiffSource.leftIdentifier)
        mergeDiffToGenerate.commonAncestorDiffSource.diffs.each {FieldDiff caSourceFieldDiff ->
            log.debug('Processing field [{}] with change type [{}] present between common ancestor and source', caSourceFieldDiff.fieldName, caSourceFieldDiff.diffType)
            switch (caSourceFieldDiff.diffType) {
                case ArrayDiff.simpleName:
                    mergeDiffToGenerate.append createArrayMergeDiffPresentOnOneSide(mergeDiffToGenerate.fullyQualifiedPath, caSourceFieldDiff as ArrayDiff)
                    break
                case FieldDiff.simpleName:
                    mergeDiffToGenerate.append createFieldMergeDiffPresentOnOneSide(mergeDiffToGenerate.fullyQualifiedPath, caSourceFieldDiff)
                    break
                default:
                    log.warn('Unhandled diff type {} on both sides', caSourceFieldDiff.diffType)
            }
        }
        mergeDiffToGenerate
    }

    def <M extends Diffable> MergeDiff<M> generateMergeDiffFromSourceAgainstTarget(MergeDiff<M> mergeDiffToGenerate) {
        log.debug('Generating merge diff from source diff against target for [{}]', mergeDiffToGenerate.sourceDiffTarget.leftIdentifier)

        mergeDiffToGenerate.sourceDiffTarget.diffs.each {FieldDiff sourceTargetFieldDiff ->
            log.debug('Processing field [{}] with change type [{}] present between source and target', sourceTargetFieldDiff.fieldName, sourceTargetFieldDiff.diffType)
            switch (sourceTargetFieldDiff.diffType) {
                case ArrayDiff.simpleName:
                    mergeDiffToGenerate.append createArrayMergeDiffFromSourceTargetArrayDiff(mergeDiffToGenerate.fullyQualifiedPath, sourceTargetFieldDiff as ArrayDiff)
                    break
                case FieldDiff.simpleName:
                    mergeDiffToGenerate.append createFieldMergeDiffFromSourceTargetFieldDiff(mergeDiffToGenerate.fullyQualifiedPath, sourceTargetFieldDiff)
                    break
                default:
                    log.warn('Unhandled diff type {} on both sides', sourceTargetFieldDiff.diffType)
            }
        }
        mergeDiffToGenerate
    }

    def <A extends Diffable> ArrayMergeDiff<A> createArrayMergeDiffFromSourceTargetArrayDiff(Path fullyQualifiedObjectPath, ArrayDiff<A> sourceTargetArrayDiff) {
        log.debug('[{}] Processing array differences against target from source', sourceTargetArrayDiff.fieldName)
        // Created and Deleted diffs in this array are left as-is as they are guaranteed to be unique and no issue
        // When the source is diff'd against the target due to the way diffing works the objects created in the source are identified as deleted and vice-versa
        // We could reverse the diff but then the modifications are the wrong way round
        arrayMergeDiff(sourceTargetArrayDiff.targetClass)
            .forFieldName(sourceTargetArrayDiff.fieldName)
            .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            .withSource(sourceTargetArrayDiff.left)
            .withTarget(sourceTargetArrayDiff.right)
            .withCommonAncestor(null)
            .withCreatedMergeDiffs(sourceTargetArrayDiff.deleted.collect {c ->
                creationMergeDiff(c.targetClass)
                    .whichCreated(c.deleted)
                    .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            })
            .withDeletedMergeDiffs(sourceTargetArrayDiff.created.collect {d ->
                deletionMergeDiff(d.targetClass)
                    .whichDeleted(d.created)
                    .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            })
            .withModifiedMergeDiffs(sourceTargetArrayDiff.modified.collect {m ->
                generateMergeDiff(
                    mergeDiff(m.targetClass)
                        .forMergingDiffable(m.left)
                        .intoDiffable(m.right)
                        .havingCommonAncestor(null)
                        .withSourceDiffedAgainstTarget(m)
                )
            })
    }

    def <A extends Diffable> ArrayMergeDiff<A> createBaseArrayMergeDiffPresentOnOneSide(Path fullyQualifiedObjectPath,
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

    def <A extends Diffable> ArrayMergeDiff<A> createArrayMergeDiffPresentOnBothSides(Path fullyQualifiedObjectPath,
                                                                                      ArrayDiff<A> caSourceArrayDiff,
                                                                                      ArrayDiff<A> caTargetArrayDiff,
                                                                                      DiffCache sourceDiffCache,
                                                                                      DiffCache targetDiffCache) {
        log.debug('[{}] Processing array differences against common ancestor on both sides inside [{}]', caSourceArrayDiff.fieldName, fullyQualifiedObjectPath)
        createBaseArrayMergeDiffPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff)
            .withTarget(caTargetArrayDiff.right)
            .withCreatedMergeDiffs(createCreationMergeDiffsPresentOnBothSides(fullyQualifiedObjectPath, caSourceArrayDiff, caTargetArrayDiff))
            .withDeletedMergeDiffs(createDeletionMergeDiffsPresentOnBothSides(fullyQualifiedObjectPath, caSourceArrayDiff.deleted, caTargetArrayDiff))
            .withModifiedMergeDiffs(
                createModifiedMergeDiffsPresentOnBothSides(fullyQualifiedObjectPath, caSourceArrayDiff, caTargetArrayDiff, sourceDiffCache, targetDiffCache))
            .getValidOnly()
    }


    def <A extends Diffable> ArrayMergeDiff<A> createArrayMergeDiffPresentOnOneSide(Path fullyQualifiedObjectPath,
                                                                                    ArrayDiff<A> caSourceArrayDiff) {
        log.debug('[{}] Processing array differences against common ancestor on one side', caSourceArrayDiff.fieldName)
        // Created and Deleted diffs in this array are left as-is as they are guaranteed to be unique and no issue
        createBaseArrayMergeDiffPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff)
            .withCreatedMergeDiffs(createCreationMergeDiffsPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff.created))
            .withDeletedMergeDiffs(createDeletionMergeDiffsPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff.deleted))
            .withModifiedMergeDiffs(createModifiedMergeDiffsPresentOnOneSide(fullyQualifiedObjectPath, caSourceArrayDiff.modified))
            .getValidOnly()
    }

    def <F> FieldMergeDiff<F> createFieldMergeDiffFromSourceTargetFieldDiff(Path fullyQualifiedObjectPath, FieldDiff<F> sourceTargetFieldDiff) {
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

    def <F> FieldMergeDiff<F> createFieldMergeDiffPresentOnBothSides(Path fullyQualifiedObjectPath, FieldDiff<F> caSourceFieldDiff, FieldDiff<F> caTargetFieldDiff) {
        log.debug('[{}] Processing field difference against common ancestor on both sides', caSourceFieldDiff.fieldName)
        createBaseFieldMergeDiffPresentOnOneSide(fullyQualifiedObjectPath, caSourceFieldDiff)
            .withTarget(caTargetFieldDiff.right)
            .asMergeConflict()
            .getValidOnly() // This is a safety check to handle when 2 diffs are used with modifications but no actual difference
    }

    def <F> FieldMergeDiff<F> createFieldMergeDiffPresentOnOneSide(Path fullyQualifiedObjectPath, FieldDiff<F> caSourceFieldDiff) {
        log.debug('[{}] Processing field difference against common ancestor on one side', caSourceFieldDiff.fieldName)
        createBaseFieldMergeDiffPresentOnOneSide(fullyQualifiedObjectPath, caSourceFieldDiff)
        // just use whats in the commonAncestor as no caTarget data denotes no difference between the CA and the target
            .withTarget(caSourceFieldDiff.left)
            .getValidOnly()
    }

    def <F> FieldMergeDiff<F> createBaseFieldMergeDiffPresentOnOneSide(Path fullyQualifiedObjectPath, FieldDiff<F> caSourceFieldDiff) {
        fieldMergeDiff(caSourceFieldDiff.targetClass)
            .forFieldName(caSourceFieldDiff.fieldName)
            .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
            .withSource(caSourceFieldDiff.right)
            .withCommonAncestor(caSourceFieldDiff.left) // both diffs have the same LHS
    }

    def <C extends Diffable> Collection<CreationMergeDiff<C>> createCreationMergeDiffsPresentOnOneSide(Path fullyQualifiedObjectPath,
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
    def <C extends Diffable> Collection<CreationMergeDiff> createCreationMergeDiffsPresentOnBothSides(Path fullyQualifiedObjectPath,
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
                log.warn('[{}] ca/source created and ca/target deleted', diff.createdIdentifier)
                //  Impossible as it didnt exist in CA therefore target can't have deleted it
                return null
            }
            if (diff.createdIdentifier in caTargetDiff.modified*.getRightIdentifier()) {
                //  Impossible as it didnt exist in CA therefore target can't have modified it
                log.warn('[{}] ca/source created and ca/target modified', diff.createdIdentifier)
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


    def <D extends Diffable> Collection<DeletionMergeDiff<D>> createDeletionMergeDiffsPresentOnOneSide(Path fullyQualifiedObjectPath,
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
    def <D extends Diffable> Collection<DeletionMergeDiff> createDeletionMergeDiffsPresentOnBothSides(Path fullyQualifiedObjectPath,
                                                                                                      Collection<DeletionDiff<D>> caSourceDeletedDiffs,
                                                                                                      ArrayDiff<D> caTargetDiff) {
        caSourceDeletedDiffs.collect {diff ->
            if (diff.deletedIdentifier in caTargetDiff.created*.createdIdentifier) {
                //  Impossible as you can't delete something which never existed
                log.warn('[{}] ca/source deleted and ca/target created', diff.deletedIdentifier)
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

    def <M extends Diffable> Collection<MergeDiff<M>> createModifiedMergeDiffsPresentOnOneSide(Path fullyQualifiedObjectPath,
                                                                                               Collection<ObjectDiff<M>> caSourceModifiedDiffs) {
        // Modified diffs represent diffs which have modifications down the chain but no changes on the target side
        // Therefore we can use an empty object diff
        caSourceModifiedDiffs.collect {objDiff ->
            Class<M> targetClass = objDiff.left.class as Class<M>
            generateMergeDiff(mergeDiff(targetClass)
                                  .forMergingDiffable(objDiff.right)
                                  .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                                  .intoDiffable(objDiff.left)
                                  .havingCommonAncestor(objDiff.left)
                                  .withCommonAncestorDiffedAgainstSource(objDiff)
            )
        }
    }

    def <M extends Diffable> Collection<MergeDiff<M>> createModifiedMergeDiffsPresentOnBothSides(Path fullyQualifiedObjectPath,
                                                                                                 ArrayDiff<M> caSourceDiff,
                                                                                                 ArrayDiff<M> caTargetDiff,
                                                                                                 DiffCache sourceDiffCache,
                                                                                                 DiffCache targetDiffCache) {

        Collection<MergeDiff<M>> modifiedDiffs = caSourceDiff.modified.collect {caSourceModifiedDiff ->
            ObjectDiff<M> caTargetModifiedDiff = caTargetDiff.modified.find {it.leftIdentifier == caSourceModifiedDiff.leftIdentifier}
            if (caTargetModifiedDiff) {
                log.debug('[{}] modified on both sides', caSourceModifiedDiff.leftIdentifier)

                MergeDiff sourceTargetMergeDiff = generateMergeDiff(mergeDiff(caSourceModifiedDiff.targetClass)
                                                                        .forMergingDiffable(caSourceModifiedDiff.right)
                                                                        .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                                                                        .intoDiffable(caTargetModifiedDiff.right)
                                                                        .havingCommonAncestor(caSourceModifiedDiff.left)
                                                                        .withCommonAncestorDiffedAgainstSource(caSourceModifiedDiff)
                                                                        .withCommonAncestorDiffedAgainstTarget(caTargetModifiedDiff)
                )
                // If no diffs then the modifications are the same so dont include
                return sourceTargetMergeDiff.isEmpty() ? null : sourceTargetMergeDiff

            }
            DeletionDiff<M> caTargetDeletionDiff = caTargetDiff.deleted.find {it.deletedIdentifier == caSourceModifiedDiff.leftIdentifier}
            if (caTargetDeletionDiff) {
                log.debug('[{}] modified on ca/source side and deleted on ca/target side. TREATED AS CREATION', caSourceModifiedDiff.leftIdentifier)
                return null
            }

            log.debug('[{}] only modified on ca/source side', caSourceModifiedDiff.leftIdentifier)
            generateMergeDiff(mergeDiff(caSourceModifiedDiff.targetClass)
                                  .forMergingDiffable(caSourceModifiedDiff.right)
                                  .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                                  .intoDiffable(caSourceModifiedDiff.left)
                                  .havingCommonAncestor(caSourceModifiedDiff.left)
                                  .withCommonAncestorDiffedAgainstSource(caSourceModifiedDiff)
            )
        }.findAll()


        Collection<MergeDiff<M>> createdModifiedDiffs = caSourceDiff.created.collect {caSourceCreationDiff ->
            CreationDiff<M> caTargetCreationDiff = caTargetDiff.created.find {it.createdIdentifier == caSourceCreationDiff.createdIdentifier}
            if (caTargetCreationDiff) {
                // Both sides added : potential conflict and therefore is a modified rather than create or no diff
                log.debug('[{}] ca/source created exists in ca/target created. This is a potential merge modification', caSourceCreationDiff.createdIdentifier)

                // Get the diff of the 2 objects, we need to determine if theres actually a merge conflict
                M sourceCreated = caSourceCreationDiff.created
                M targetCreated = caTargetCreationDiff.created
                DiffCache sourceCache = sourceDiffCache.getDiffCache(sourceCreated.getPath())
                DiffCache targetCache = targetDiffCache.getDiffCache(targetCreated.getPath())
                ObjectDiff<M> sourceTargetDiff = getMergeContextDiffForCreatedItems(sourceCreated, targetCreated, sourceCache, targetCache)
                // If objects are identical then theres no merge difference so it can be ignored
                if (sourceTargetDiff.objectsAreIdentical()) {
                    log.debug('Both sides created but the creations are identical')
                    return null
                }

                return generateMergeDiff(mergeDiff(sourceTargetDiff.targetClass as Class<M>)
                                             .forMergingDiffable(caSourceCreationDiff.created)
                                             .insideFullyQualifiedObjectPath(fullyQualifiedObjectPath)
                                             .intoDiffable(caTargetCreationDiff.created)
                                             .havingCommonAncestor(null)
                                             .withSourceDiffedAgainstTarget(sourceTargetDiff)
                )
            }
            null
        }.findAll()
        modifiedDiffs.addAll(createdModifiedDiffs)
        modifiedDiffs
    }

    @CompileDynamic
    def <M extends Diffable> ObjectDiff<M> getMergeContextDiffForCreatedItems(M sourceCreated, M targetCreated, DiffCache sourceDiffCache, DiffCache targetDiffCache) {
        try {
            return sourceCreated.diff(targetCreated, 'merge', sourceDiffCache, targetDiffCache)
        } catch (LazyInitializationException ignored) {
            MdmDomainService service = findMdmDomainServiceService(sourceCreated.domainType)
            return getMergeContextDiffForCreatedItems(service.get(sourceCreated.id), service.get(targetCreated.id), sourceDiffCache, targetDiffCache) as ObjectDiff<M>
        }

    }

    MdmDomainService findMdmDomainServiceService(String domainType) {
        MdmDomainService service = mdmDomainServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('MD01', "No supporting service for ${domainType}")
        return service
    }
}
