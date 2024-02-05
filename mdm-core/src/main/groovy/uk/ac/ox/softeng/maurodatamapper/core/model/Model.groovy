/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.VersionLinkAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

import grails.gorm.DetachedCriteria
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.query.criteria.AbstractDetachedCriteria

/**
 * Base class for all models which are contained inside a folder. These are securable resources.
 *
 * @since 04/11/2019
 */
@SelfType(GormEntity)
trait Model<D extends Diffable> extends CatalogueItem<D> implements SecurableResource, Comparable<D>, VersionLinkAware, VersionAware {

    Folder folder
    Boolean deleted
    Boolean readableByEveryone
    Boolean readableByAuthenticatedUsers
    String modelType
    String author
    String organisation
    Authority authority

    static belongsTo = Authority

    static constraints = {
        [authority, label] unique: true
    }

    static mapping = {
    }

    @Override
    void setFinalised(Boolean finalised) {
        super.setFinalised(finalised)
        this.breadcrumbTree?.finalised = finalised
    }

    @Override
    String getPathIdentifier() {
        label ? "${label}${PathNode.MODEL_PATH_IDENTIFIER_SEPARATOR}${modelVersion ?: branchName}" : null
    }

    @Override
    int compareTo(D that) {
        int res = 0
        if (that instanceof CatalogueItem) {
            if (that.class != this.class) res = this.order <=> that.order
            if (res == 0) res = this.label <=> that.label
        }
        if (that instanceof Model) {
            res = res == 0 ? this.documentationVersion <=> that.documentationVersion : res
            res = res == 0 ? this.modelVersion <=> that.modelVersion : res
            res = res == 0 ? this.branchName <=> that.branchName : res
        }
        res
    }

    Path getFullPathInsideFolder() {
        Path.from(folder.path, path)
    }

    static <T extends Model> DetachedCriteria<T> withReadable(DetachedCriteria<T> criteria, List<UUID> readableIds, Boolean includeDeleted) {
        if (!includeDeleted) criteria = withNotDeleted(criteria)
        criteria.inList('id', readableIds.toList())
    }

    static <T extends Model> DetachedCriteria<T> withNotDeleted(DetachedCriteria<T> criteria,
                                                                @DelegatesTo(AbstractDetachedCriteria) Closure additionalSecurity = null) {
        criteria.and {
            eq('deleted', false)
            if (additionalSecurity) {
                additionalSecurity.delegate = criteria
                additionalSecurity.call()
            }
        }

    }

    static <T extends Model> DetachedCriteria<T> byContainerIdInList(String containerPropertyName, Collection<UUID> containerIds) {
        by().inList("${containerPropertyName}.id", containerIds)
    }

    static <T extends Model> DetachedCriteria<T> byFolderId(UUID folderId) {
        by()
        .eq('folder.id', folderId)
    }

    static <T extends Model> DetachedCriteria<T> byFolderIdInList(Collection<UUID> folderIds) {
        by().inList('folder.id', folderIds)
    }

    static <T extends Model> DetachedCriteria<T> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static <T extends Model> DetachedCriteria<T> byDeleted() {
        by()
        .eq('deleted', true)
    }

    static <T extends Model> DetachedCriteria<T> byIdInList(Collection<UUID> ids) {
        by()
        .inList('id', ids.toList())
    }

    static <T extends Model> DetachedCriteria<T> byLabel(String label) {
        by()
        .eq('label', label)
    }

    static <T extends Model> DetachedCriteria<T> byLabelAndBranchNameAndFinalised(String label, String branchName) {
        byLabel(label)
        .eq('branchName', branchName)
        .eq('finalised', true)
    }

    static <T extends Model> DetachedCriteria<T> byLabelAndNotFinalised(String label) {
        byLabel(label)
        .eq('finalised', false)
    }

    static <T extends Model> DetachedCriteria<T> byLabelAndNotFinalisedAndIdNotEqual(String label, UUID id) {
        byLabelAndNotFinalised()
        .ne('id', id)
    }

    static <T extends Model> DetachedCriteria<T> byLabelAndBranchNameAndNotFinalised(String label, String branchName) {
        byLabelAndNotFinalised(label)
        .eq('branchName', branchName)
    }
}
