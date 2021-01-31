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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.VersionedFolderLabelValidator
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi

class VersionedFolder extends Folder implements VersionAware {

    Authority authority

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        CallableConstraints.call(VersionAwareConstraints, delegate)

        label validator: { val, obj -> new VersionedFolderLabelValidator(obj).isValid(val) }
        parentFolder nullable: true
    }

    static mapping = {
        documentationVersion type: VersionUserType
        modelVersion type: VersionUserType
    }

    VersionedFolder() {
        super()
        initialiseVersioning()
    }

    @Override
    String getDomainType() {
        VersionedFolder.simpleName
    }

    static DetachedCriteria<VersionedFolder> by() {
        new DetachedCriteria<VersionedFolder>(VersionedFolder)
    }

    static DetachedCriteria<VersionedFolder> byLabel(String label) {
        by().eq('label', label)
    }

    static DetachedCriteria<VersionedFolder> byNoParentFolder() {
        by().isNull('parentFolder')
    }

    static DetachedCriteria<VersionedFolder> byParentFolderId(UUID id) {
        by().eq('parentFolder.id', id)
    }

    static DetachedCriteria<VersionedFolder> byParentFolderIdAndLabel(UUID id, String label) {
        byParentFolderId(id).eq('label', label)
    }

    static VersionedFolder findOrCreateByLabel(String label, Map creationMap) {
        VersionedFolder folder = findByLabel(label)
        if (folder) return folder

        creationMap.label = label
        folder = new VersionedFolder(creationMap)
        folder.save(flush: true, validate: false)
    }

    static VersionedFolder findOrCreateByLabel(String label, User creator, String description,
                                               Boolean readableByAuthenticated) {
        findOrCreateByLabel(label, [description            : description,
                                    createdBy              : creator,
                                    readableByAuthenticated: readableByAuthenticated])
    }

    static List<VersionedFolder> luceneList(@DelegatesTo(HibernateSearchApi) Closure closure) {
        VersionedFolder.search().list closure
    }

    static List<VersionedFolder> findAllWithIdsInPath(List<String> ids) {
        by()
            .isNotNull('path')
            .ne('path', '')
            .findAll { f ->
                ids.any {
                    it in f.path.split('/')
                }
            }
    }

    static List<VersionedFolder> findAllContainedInFolderId(UUID folderId) {
        luceneList {
            should {
                keyword 'path', folderId.toString()
            }
        }
    }

    static List<VersionedFolder> luceneTreeLabelSearch(List<String> allowedIds, String searchTerm) {
        if (!allowedIds) return []
        luceneList {
            keyword 'label', searchTerm
            filter name: 'idSecured', params: [allowedIds: allowedIds]
        }
    }
}
