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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.VersionLinkAware
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.query.criteria.AbstractDetachedCriteria

import java.time.OffsetDateTime

/**
 * Base class for all models which are contained inside a folder. These are securable resources.
 *
 * @since 04/11/2019
 */
@SelfType(GormEntity)
trait Model<D extends Diffable> extends CatalogueItem<D> implements SecurableResource, Comparable<D>, VersionLinkAware {

    Folder folder
    Boolean deleted
    Boolean readableByEveryone
    Boolean readableByAuthenticatedUsers
    Boolean finalised
    String modelType
    String author
    String organisation
    OffsetDateTime dateFinalised
    Authority authority

    static belongsTo = Authority

    static constraints = {
        [authority, label] unique: true
    }

    static mapping = {
        authority default: Authority.findByLabel('Mauro Data Mapper')
    }

    @BindUsing({obj, source -> Version.from(source['documentationVersion'] as String)})
    Version documentationVersion

    abstract Boolean hasChildren()

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

    static def <T extends Model> ObjectDiff modelDiffBuilder(Class<T> diffClass, T lhs, T rhs) {
        catalogueItemDiffBuilder(diffClass, lhs, rhs)
            .appendBoolean('deleted', lhs.deleted, rhs.deleted)
            .appendBoolean('finalised', lhs.finalised, rhs.finalised)
            .appendString('modelType', lhs.modelType, rhs.modelType)
            .appendString('author', lhs.author, rhs.author)
            .appendString('organisation', lhs.organisation, rhs.organisation)
            .appendString('documentationVersion', lhs.documentationVersion.toString(), rhs.documentationVersion.toString())
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
}
