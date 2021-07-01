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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.VersionLinkAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
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
    String getPathIdentifier() {
        "${label}.${modelVersion ?: branchName}"
    }

    @Override
    int compareTo(D that) {
        int res = 0
        if (that instanceof CatalogueItem) {
            if (that.class != this.class) res = this.order <=> that.order
            if (res == 0) this.label <=> that.label
        }
        if (that instanceof Model) {
            res == 0 ? this.documentationVersion <=> that.documentationVersion : res
        }
        res
    }

    static <T extends Model> ObjectDiff modelDiffBuilder(Class<T> diffClass, T lhs, T rhs) {
        catalogueItemDiffBuilder(diffClass, lhs, rhs)
            .appendBoolean('deleted', lhs.deleted, rhs.deleted)
            .appendBoolean('finalised', lhs.finalised, rhs.finalised)
            .appendString('modelType', lhs.modelType, rhs.modelType)
            .appendString('author', lhs.author, rhs.author)
            .appendString('organisation', lhs.organisation, rhs.organisation)
            .appendString('documentationVersion', lhs.documentationVersion.toString(), rhs.documentationVersion.toString())
            .appendString('modelVersion', lhs.modelVersion.toString(), rhs.modelVersion.toString())
            .appendString('branchName', lhs.branchName, rhs.branchName)
            .appendOffsetDateTime('dateFinalised', lhs.dateFinalised, rhs.dateFinalised)
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

    static <T extends Model> DetachedCriteria<T> byFolderId(UUID folderId) {
        by()
        .eq('folder.id', folderId)
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

    static <T extends Model> DetachedCriteria<T> byLabelAndFinalisedAndLatestModelVersion(String label) {
        byLabel(label)
        .eq('finalised', true)
        .order('modelVersion', 'desc')
    }

    static <T extends Model> DetachedCriteria<T> byLabelAndBranchNameAndFinalisedAndLatestModelVersion(String label, String branchName) {
        byLabelAndFinalisedAndLatestModelVersion(label)
        .eq('branchName', branchName)
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
