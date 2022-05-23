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
package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Metadata implements MultiFacetItemAware, Diffable<Metadata> {

    public final static Integer BATCH_SIZE = 1000

    UUID id

    String namespace
    String key
    String value

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        multiFacetAwareItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.multiFacetAwareItem && !obj.multiFacetAwareItem.ident()) return true
            ['default.null.message']
        }
        namespace blank: false
        key blank: false
        value blank: false
    }

    static mapping = {
        batchSize(10)
        namespace type: 'text'
        key type: 'text'
        value type: 'text'
        multiFacetAwareItemId index: 'metadata_catalogue_item_idx'
    }

    static search = {
        namespace searchable: 'yes', analyze: false
        key searchable: 'yes', analyzer: 'wordDelimiter'
        value searchable: 'yes'
    }

    static transients = ['multiFacetAwareItem']

    Metadata() {
    }

    @Override
    String getDomainType() {
        Metadata.simpleName
    }

    @Override
    String getPathPrefix() {
        'md'
    }

    @Override
    String toString() {
        "${getClass().getName()} : ${namespace}/${key} : ${id ?: '(unsaved)'}"
    }

    def beforeValidate() {
        value = value ?: 'N/A'
        //        beforeValidateCheck()
    }

    @Override
    def beforeInsert() {
        beforeInsertCheck()
    }

    @Override
    String getEditLabel() {
        "Metadata:${namespace}:${key}"
    }

    @Override
    ObjectDiff<Metadata> diff(Metadata that, String context) {
        diff(that, context, null, null)
    }

    @Override
    ObjectDiff<Metadata> diff(Metadata that, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.objectDiff(Metadata)
            .leftHandSide(id.toString(), this)
            .rightHandSide(that.id.toString(), that)
            .withLeftHandSideCache(lhsDiffCache)
            .withRightHandSideCache(rhsDiffCache)
            .appendString('namespace', this.namespace, that.namespace)
            .appendString('key', this.key, that.key)
            .appendString('value', this.value, that.value)
    }

    @Override
    String getPathIdentifier() {
        "${this.namespace}.${this.key}"
    }

    static DetachedCriteria<Metadata> distinctNamespacesKeys() {
        new DetachedCriteria<Metadata>(Metadata)
            .projections {
                groupProperty('namespace')
                groupProperty('key')
            }
            .order('namespace', 'asc')
            .order('key', 'asc')
    }

    static Map<String, List<String>> findAllDistinctNamespacesKeys() {
        (distinctNamespacesKeys().list() as List<List<String>>)
            .groupBy {it[0]}
            .collectEntries {k, v ->
                [k, v.flatten().findAll {it != k} as List<String>]
            } as Map<String, List<String>>
    }

    static Map<String, List<String>> findAllDistinctNamespacesKeysIlikeNamespace(String namespacePrefix) {
        (distinctNamespacesKeys()
            .ilike('namespace', "${namespacePrefix}%")
            .list() as List<List<String>>)
            .groupBy {it[0]}
            .collectEntries {k, v ->
                [k, v.flatten().findAll {it != k} as List<String>]
            } as Map<String, List<String>>
    }

    static Set<String> findAllDistinctKeysByNamespace(String namespace) {
        new DetachedCriteria<Metadata>(Metadata).eq('namespace', namespace)
            .distinct('key')
            .list() as Set<String>
    }

    static Set<String> findAllDistinctNamespaces() {
        new DetachedCriteria<Metadata>(Metadata)
            .distinct('namespace')
            .list() as Set<String>
    }

    static Set<String> findAllDistinctNamespacesIlike(String namespacePrefix) {
        new DetachedCriteria<Metadata>(Metadata)
            .distinct('namespace')
            .ilike('namespace', "${namespacePrefix}%")
            .list() as Set<String>
    }

    static DetachedCriteria<Metadata> by() {
        new DetachedCriteria<Metadata>(Metadata)
    }

    static DetachedCriteria<Metadata> byMultiFacetAwareItemId(Serializable multiFacetAwareItemId, Map filters = [:]) {
        DetachedCriteria criteria = new DetachedCriteria<Metadata>(Metadata).eq('multiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId))
        if (filters) {
            criteria = withFilter(criteria, filters)
        }

        criteria
    }

    static DetachedCriteria<Metadata> byMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        new DetachedCriteria<Metadata>(Metadata).inList('multiFacetAwareItemId', multiFacetAwareItemIds)
    }

    static DetachedCriteria<Metadata> byMultiFacetAwareItemIdAndId(Serializable multiFacetAwareItemId, Serializable resourceId) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<Metadata> byNamespace(String namespace) {
        new DetachedCriteria<Metadata>(Metadata).eq('namespace', namespace)
    }

    static DetachedCriteria<Metadata> byNamespaceAndKey(String namespace, String key) {
        new DetachedCriteria<Metadata>(Metadata).eq('namespace', namespace).eq('key', key)
    }

    static DetachedCriteria<Metadata> byNamespaceAndKeyAndValue(String namespace, String key, String value) {
        byNamespaceAndKey(namespace, key).eq('value', value)
    }

    static DetachedCriteria<Metadata> byMultiFacetAwareItemIdAndNamespace(Serializable multiFacetAwareItemId, String namespace) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).eq('namespace', namespace)
    }

    static DetachedCriteria<Metadata> byMultiFacetAwareItemIdAndNotNamespaces(Serializable multiFacetAwareItemId, List<String> namespaces, Map filters = [:]) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).not {inList('namespace', namespaces)}
    }

    static DetachedCriteria<Metadata> byMultiFacetAwareItemIdAndNamespaceNotLike(Serializable multiFacetAwareItemId,
                                                                                 String notLikeNamespace, Map filters = [:]) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).not {
            like 'namespace', notLikeNamespace
        }
    }

    static DetachedCriteria<Metadata> byMultiFacetAwareItemIdAndNotNamespacesAndNamespaceNotLike(Serializable multiFacetAwareItemId, List<String> namespaces,
                                                                                                 String notLikeNamespace, Map filters = [:]) {
        byMultiFacetAwareItemIdAndNotNamespaces(multiFacetAwareItemId, namespaces).not {
            like 'namespace', notLikeNamespace
        }
    }


    static DetachedCriteria<Metadata> withFilter(DetachedCriteria<Metadata> criteria, Map filters) {
        if (filters.ns) criteria = criteria.ilike('namespace', "%${filters.ns}%")
        if (filters.key) criteria = criteria.ilike('key', "%${filters.key}%")
        if (filters.value) criteria = criteria.ilike('value', "%${filters.value}%")
        criteria
    }
}