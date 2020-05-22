/*
 * Copyright 2020 University of Oxford
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

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.FolderLabelValidator
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.search.PathTokenizerAnalyzer
import uk.ac.ox.softeng.maurodatamapper.search.bridge.OffsetDateTimeBridge
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Folder implements Container {

    public static final String MISCELLANEOUS_FOLDER_LABEL = 'Miscellaneous'
    public static final String DEFAULT_FOLDER_LABEL = 'New Folder'

    UUID id
    Boolean deleted

    Folder parentFolder

    static hasMany = [
        childFolders: Folder,
    ]

    static belongsTo = [Folder]

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        label validator: {val, obj -> new FolderLabelValidator(obj).isValid(val)}
        parentFolder nullable: true
    }

    static mapping = {
        parentFolder index: 'folder_parent_folder_idx'
        childFolders cascade: 'all-delete-orphan'
    }

    static mappedBy = [
        childFolders: 'parentFolder',
    ]

    static search = {
        label index: 'yes', analyzer: 'wordDelimiter'
        path index: 'yes', analyzer: PathTokenizerAnalyzer
        description termVector: 'with_positions'
        lastUpdated index: 'yes', bridge: ['class': OffsetDateTimeBridge]
        dateCreated index: 'yes', bridge: ['class': OffsetDateTimeBridge]
    }

    Folder() {
        deleted = false
    }

    @Override
    String getDomainType() {
        Folder.simpleName
    }


    boolean hasChildFolders() {
        Folder.countByParentFolder(this)
    }

    @Override
    Folder getPathParent() {
        parentFolder
    }

    @Override
    def beforeValidate() {
        buildPath()
        childFolders.each {it.beforeValidate()}
    }

    @Override
    def beforeInsert() {
        buildPath()
    }

    @Override
    def beforeUpdate() {
        buildPath()
    }

    @Override
    void addCreatedEdit(User creator) {
        String description = parentFolder ? "${editLabel} added as child of [${parentFolder.editLabel}]" : "${editLabel} added"
        addToEditsTransactionally creator, description
    }

    @Override
    String getEditLabel() {
        "Folder:${label}"
    }

    @Override
    boolean hasChildren() {
        !childFolders.isEmpty()
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
        by().eq('parentFolder.id', id)
    }

    static Folder getMiscellaneousFolder() {
        findByLabel(MISCELLANEOUS_FOLDER_LABEL)
    }

    static Folder findOrCreateByLabel(String label, Map creationMap) {
        Folder folder = findByLabel(label)
        if (folder) return folder

        creationMap.label = label
        folder = new Folder(creationMap)
        folder.save(flush: true, validate: false)
    }

    static Folder findOrCreateByLabel(String label, User creator, String description,
                                      Boolean readableByAuthenticated) {
        findOrCreateByLabel(label, [description            : description,
                                    createdBy              : creator,
                                    readableByAuthenticated: readableByAuthenticated])
    }

    static List<Folder> luceneList(@DelegatesTo(HibernateSearchApi) Closure closure) {
        Folder.search().list closure
    }

    static List<Folder> findAllWithIdsInPath(List<String> ids) {
        Folder.by()
            .isNotNull('path')
            .ne('path', '')
        //      .list()
            .findAll {f ->
                ids.any {
                    it in f.path.split('/')
                }
            }
        //        luceneList {
        //            should {
        //                ids.each {
        //                    keyword 'path', it
        //                }
        //            }
        //        }
    }

    static List<Folder> findAllContainedInFolderId(UUID folderId) {
        luceneList {
            should {
                keyword 'path', folderId.toString()
            }
        }
    }

    static List<Folder> luceneTreeLabelSearch(List<String> allowedIds, String searchTerm) {
        luceneList {
            keyword 'label', searchTerm
            filter name: 'idSecured', params: [allowedIds: allowedIds]
        }
    }
}
