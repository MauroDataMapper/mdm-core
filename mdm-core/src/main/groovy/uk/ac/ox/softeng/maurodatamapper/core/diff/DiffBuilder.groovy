/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
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
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class DiffBuilder {

    static <K extends Diffable> ArrayDiff<K> arrayDiff(Class<Collection<K>> arrayClass) {
        new ArrayDiff<K>(arrayClass)
    }

    static <F> FieldDiff<F> fieldDiff(Class<F> fieldClass) {
        new FieldDiff<F>(fieldClass)
    }

    static <K extends Diffable> ObjectDiff<K> objectDiff(Class<K> objectClass) {
        new ObjectDiff<K>(objectClass)
    }

    static <K extends Diffable> MergeDiff<K> mergeDiff(Class<K> objectClass) {
        new MergeDiff<K>(objectClass)
    }

    static <K extends Diffable> CreationDiff<K> creationDiff(Class<K> objectClass) {
        new CreationDiff<K>(objectClass)
    }

    static <K extends Diffable> DeletionDiff<K> deletionDiff(Class<K> objectClass) {
        new DeletionDiff<K>(objectClass)
    }

    static <K extends Diffable> ArrayMergeDiff<K> arrayMergeDiff(Class<Collection<K>> arrayClass) {
        new ArrayMergeDiff<K>(arrayClass)
    }

    static <F> FieldMergeDiff<F> fieldMergeDiff(Class<F> fieldClass) {
        new FieldMergeDiff<F>(fieldClass)
    }

    static <K extends Diffable> CreationMergeDiff<K> creationMergeDiff(Class<K> objectClass) {
        new CreationMergeDiff<K>(objectClass)
    }

    static <K extends Diffable> DeletionMergeDiff<K> deletionMergeDiff(Class<K> objectClass) {
        new DeletionMergeDiff<K>(objectClass)
    }

    @Deprecated
    static <T extends CatalogueItem> ObjectDiff catalogueItemDiffBuilder(Class<T> diffClass, T lhs, T rhs) {
        catalogueItemDiffBuilder(diffClass, lhs, rhs, null, null)
    }

    static <T extends CatalogueItem> ObjectDiff catalogueItemDiffBuilder(Class<T> diffClass, T lhs, T rhs, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        String lhsId = lhs.id ?: "Left:Unsaved_${lhs.domainType}"
        String rhsId = rhs.id ?: "Right:Unsaved_${rhs.domainType}"
        ObjectDiff<T> baseDiff = objectDiff(diffClass)
            .leftHandSide(lhsId, lhs)
            .rightHandSide(rhsId, rhs)
            .appendString('label', lhs.label, rhs.label)
            .appendString('description', lhs.description, rhs.description)
            .appendString('aliasesString', lhs.aliasesString, rhs.aliasesString)

        // If no cache then use the persistent relationships
        if (!lhsDiffCache || !rhsDiffCache) {
            log.warn('Building CI [{}] diff without cache', lhs.path)
            return baseDiff
                .appendCollection(Metadata, 'metadata', lhs.metadata, rhs.metadata)
                .appendCollection(Annotation, 'annotations', lhs.annotations, rhs.annotations)
                .appendCollection(Rule, 'rules', lhs.rules, rhs.rules)
        }
        baseDiff
            .withLeftHandSideCache(lhsDiffCache)
            .withRightHandSideCache(rhsDiffCache)
            .appendCollection(Metadata, 'metadata')
            .appendCollection(Annotation, 'annotations')
            .appendCollection(Rule, 'rules')
    }

    @Deprecated
    static <T extends Model> ObjectDiff modelDiffBuilder(Class<T> diffClass, T lhs, T rhs) {
        modelDiffBuilder(diffClass, lhs, rhs, null, null)
    }

    static <T extends Model> ObjectDiff modelDiffBuilder(Class<T> diffClass, T lhs, T rhs, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {

        catalogueItemDiffBuilder(diffClass, lhs, rhs, lhsDiffCache, rhsDiffCache)
            .appendBoolean('deleted', lhs.deleted, rhs.deleted)
            .appendBoolean('finalised', lhs.finalised, rhs.finalised)
            .appendString('modelType', lhs.modelType, rhs.modelType)
            .appendString('author', lhs.author, rhs.author)
            .appendString('organisation', lhs.organisation, rhs.organisation)
            .appendString('documentationVersion', lhs.documentationVersion.toString(), rhs.documentationVersion.toString())
            .appendString('modelVersion', lhs.modelVersion?.toString(), rhs.modelVersion?.toString())
            .appendString('branchName', lhs.branchName, rhs.branchName)
            .appendOffsetDateTime('dateFinalised', lhs.dateFinalised, rhs.dateFinalised)
    }

    static <T extends Folder> ObjectDiff<T> folderDiffBuilder(Class<T> diffClass, T lhs, T rhs, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        String lhsId = lhs.id ?: "Left:Unsaved_${lhs.domainType}"
        String rhsId = rhs.id ?: "Right:Unsaved_${rhs.domainType}"
        ObjectDiff<T> baseDiff = objectDiff(diffClass)
            .leftHandSide(lhsId, lhs)
            .rightHandSide(rhsId, rhs)
            .appendString('label', lhs.label, rhs.label)
            .appendString('description', lhs.description, rhs.description)
            .appendBoolean('deleted', lhs.deleted, rhs.deleted)

        // If no cache then use the persistent relationships
        if (!lhsDiffCache || !rhsDiffCache) {
            return baseDiff
                .appendCollection(Metadata, 'metadata', lhs.metadata, rhs.metadata)
                .appendCollection(Annotation, 'annotations', lhs.annotations, rhs.annotations)
                .appendCollection(Rule, 'rules', lhs.rules, rhs.rules)
            // Add no matter what so we can iterate through to add models
                .appendCollection(Folder, 'folders', lhs.childFolders, rhs.childFolders, null, true)
        }
        baseDiff
            .withLeftHandSideCache(lhsDiffCache)
            .withRightHandSideCache(rhsDiffCache)
            .appendCollection(Metadata, 'metadata')
            .appendCollection(Annotation, 'annotations')
            .appendCollection(Rule, 'rules')
        // Add no matter what so we can iterate through to add models
            .appendCollection(Folder, 'folders', null, true)
    }
}
