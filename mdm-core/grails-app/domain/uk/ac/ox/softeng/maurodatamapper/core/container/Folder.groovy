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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.FolderLabelValidator
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueValuesValidator
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.IdSecureFilterFactory

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Folder implements Container, Diffable<Folder> {

    public static final String MISCELLANEOUS_FOLDER_LABEL = 'Miscellaneous'
    public static final String DEFAULT_FOLDER_LABEL = 'New Folder'

    UUID id
    Boolean deleted
    Boolean readableByEveryone
    Boolean readableByAuthenticatedUsers
    List<Map> groups
    static transients = ['groups']

    Folder parentFolder

    static hasMany = [
        childFolders  : Folder,
        metadata      : Metadata,
        annotations   : Annotation,
        semanticLinks : SemanticLink,
        referenceFiles: ReferenceFile,
        rules         : Rule
    ]

    static belongsTo = [Folder]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        label validator: {val, obj -> new FolderLabelValidator(obj).isValid(val)}
        parentFolder nullable: true
        metadata validator: {val, obj ->
            if (val) new UniqueValuesValidator('namespace:key').isValid(val.groupBy {"${it.namespace}:${it.key}"})
        }
    }

    static mapping = {
        parentFolder index: 'folder_parent_folder_idx', cascadeValidate: 'none'
        childFolders cascade: 'all-delete-orphan'
    }

    static mappedBy = [
        childFolders: 'parentFolder',
    ]

    static search = {
        label searchable: 'yes', analyzer: 'wordDelimiter'
        path searchable: 'yes', analyzer: 'path'
        description termVector: 'with_positions'
        lastUpdated searchable: 'yes'
        dateCreated searchable: 'yes'
    }

    Folder() {
        deleted = false
        readableByAuthenticatedUsers = false
        readableByEveryone = false
    }

    @Override
    String getDomainType() {
        Folder.simpleName
    }

    @Override
    ObjectDiff<Folder> diff(Folder that, String context) {
        folderDiffBuilder(Folder, this, that)
    }

    static <T extends Folder> ObjectDiff<T> folderDiffBuilder(Class<T> diffClass, T lhs, T rhs) {
        String lhsId = lhs.id ?: "Left:Unsaved_${lhs.domainType}"
        String rhsId = rhs.id ?: "Right:Unsaved_${rhs.domainType}"
        DiffBuilder.objectDiff(diffClass)
            .leftHandSide(lhsId, lhs)
            .rightHandSide(rhsId, rhs)
            .appendString('label', lhs.label, rhs.label)
            .appendString('description', lhs.description, rhs.description)
            .appendList(Metadata, 'metadata', lhs.metadata, rhs.metadata)
            .appendList(Annotation, 'annotations', lhs.annotations, rhs.annotations)
            .appendList(Rule, 'rule', lhs.rules, rhs.rules)
            .appendBoolean('deleted', lhs.deleted, rhs.deleted)
        // Add no matter what so we can iterate through to add models
            .appendList(Folder, 'folders', lhs.childFolders, rhs.childFolders, null, true)
    }

    @Override
    String getPathPrefix() {
        'fo'
    }

    @Override
    String getPathIdentifier() {
        label
    }

    boolean hasChildFolders() {
        Folder.countByParentFolder(this)
    }

    def beforeValidate() {
        childFolders.each {it.beforeValidate()}
    }

    @Override
    String getCreatedEditDescription() {
        parentFolder ? "[${editLabel}] added as child of [${parentFolder.editLabel}]" : "[$editLabel] created"
    }

    @Override
    String getEditLabel() {
        "${domainType}:${label}"
    }

    @Override
    boolean hasChildren() {
        !childFolders.isEmpty()
    }

    @Override
    Container getParent() {
        parentFolder
    }

    static DetachedCriteria<Folder> by() {
        new DetachedCriteria<Folder>(Folder)
    }

    static DetachedCriteria<Folder> byLabel(String label) {
        by().eq('label', label)
    }

    static DetachedCriteria<Folder> byNoParentFolder() {
        by().isNull('parentFolder')
    }

    static DetachedCriteria<Folder> byParentFolderId(UUID id) {
        id ?
        by().eq('parentFolder.id', id) :
        by().isNull('parentFolder')
    }

    static DetachedCriteria<Folder> byParentFolderIdAndLabel(UUID id, String label) {
        if (id) {
            byParentFolderId(id).eq('label', label)
        } else {
            byNoParentFolder().eq('label', label)
        }
    }

    static Folder getMiscellaneousFolder() {
        findByLabel(MISCELLANEOUS_FOLDER_LABEL)
    }

    static List<Folder> luceneList(@DelegatesTo(HibernateSearchApi) Closure closure) {
        Folder.search().list closure
    }

    static List<Folder> findAllContainedInFolderId(UUID folderId) {
        luceneList {
            should {
                keyword 'path', folderId.toString()
            }
        }
    }

    static List<Folder> luceneTreeLabelSearch(List<String> allowedIds, String searchTerm) {
        if (!allowedIds) return []
        luceneList {
            keyword 'label', searchTerm
            filter IdSecureFilterFactory.createFilterPredicate(searchPredicateFactory, allowedIds)
        }
    }

    static DetachedCriteria<Folder> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<Folder> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}
