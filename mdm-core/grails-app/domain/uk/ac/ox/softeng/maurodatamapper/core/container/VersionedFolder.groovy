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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.VersionedFolderLabelValidator
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.VersionLinkAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.IdSecureFilterFactory
import uk.ac.ox.softeng.maurodatamapper.path.PathNode

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi

class VersionedFolder extends Folder implements VersionAware, VersionLinkAware, Diffable<VersionedFolder> {

    Authority authority

    static hasMany = [
        versionLinks: VersionLink,
    ]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        CallableConstraints.call(VersionAwareConstraints, delegate)

        label validator: { val, obj -> new VersionedFolderLabelValidator(obj).isValid(val) }
        parentFolder nullable: true
        childFolders validator: { val, obj ->
            if (obj.ident()) {
                return VersionedFolder.countByParentFolder(obj) ? ['Cannot have any VersionedFolders inside a VersionedFolder'] : true
            }
            val.any { it.domainType == VersionedFolder.simpleName } ? ['Cannot have any VersionedFolders inside a VersionedFolder'] : true
        }
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

    @Override
    ObjectDiff<VersionedFolder> diff(VersionedFolder that, String context) {
        diff(that, context, null, null)
    }

    @Override
    ObjectDiff<VersionedFolder> diff(VersionedFolder that, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.folderDiffBuilder(VersionedFolder, this, that, lhsDiffCache, rhsDiffCache)
            .appendBoolean('finalised', this.finalised, that.finalised)
            .appendString('documentationVersion', this.documentationVersion.toString(), that.documentationVersion.toString())
            .appendString('modelVersion', this.modelVersion?.toString(), that.modelVersion?.toString())
            .appendString('branchName', this.branchName, that.branchName)
            .appendOffsetDateTime('dateFinalised', this.dateFinalised, that.dateFinalised)
    }

    @Override
    String getPathPrefix() {
        'vf'
    }

    @Override
    String getPathIdentifier() {
        "${label}${PathNode.MODEL_PATH_IDENTIFIER_SEPARATOR}${modelVersion ?: branchName}"
    }

    static DetachedCriteria<VersionedFolder> by() {
        new DetachedCriteria<VersionedFolder>(VersionedFolder)
    }

    static DetachedCriteria<VersionedFolder> byLabel(String label) {
        by().eq('label', label)
    }

    static DetachedCriteria<VersionedFolder> byLabelAndFinalisedAndLatestModelVersion(String label) {
        byLabel(label)
            .eq('finalised', true)
            .order('modelVersion', 'desc')
    }

    static DetachedCriteria<VersionedFolder> byLabelAndBranchNameAndFinalisedAndLatestModelVersion(String label, String branchName) {
        byLabelAndFinalisedAndLatestModelVersion(label)
            .eq('branchName', branchName)
    }

    static DetachedCriteria<VersionedFolder> byLabelAndNotFinalised(String label) {
        byLabel(label)
            .eq('finalised', false)
    }

    static DetachedCriteria<VersionedFolder> byLabelAndBranchNameAndNotFinalised(String label, String branchName) {
        byLabelAndNotFinalised(label)
            .eq('branchName', branchName)
    }

    static DetachedCriteria<VersionedFolder> byNoParentFolder() {
        by().isNull('parentFolder')
    }

    static DetachedCriteria<VersionedFolder> byParentFolderId(UUID id) {
        id ?
        by().eq('parentFolder.id', id) :
        by().isNull('parentFolder')
    }

    static DetachedCriteria<VersionedFolder> byParentFolderIdAndLabel(UUID id, String label) {
        byParentFolderId(id).eq('label', label)
    }

    static List<VersionedFolder> hibernateSearchList(@DelegatesTo(HibernateSearchApi) Closure closure) {
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
        hibernateSearchList {
            should {
                keyword 'path', folderId.toString()
            }
        }
    }

    static List<VersionedFolder> treeLabelHibernateSearch(List<String> allowedIds, String searchTerm) {
        if (!allowedIds) return []
        hibernateSearchList {
            keyword 'label', searchTerm
            filter IdSecureFilterFactory.createFilterPredicate(searchPredicateFactory, allowedIds)
        }
    }


    static DetachedCriteria<VersionedFolder> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<VersionedFolder> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

    static DetachedCriteria<VersionedFolder> byIdInList(Collection<UUID> ids) {
        by().inList('id', ids.toList())
    }

    static DetachedCriteria<VersionedFolder> withFilter(DetachedCriteria<VersionedFolder> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('label', "%${filters.label}%")
        criteria
    }
}
