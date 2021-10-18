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

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.model.container.CatalogueItemClassifierAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.HibernateSearch
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.transform.SelfType
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity

/**
 * @since 06/12/2019
 */
@Slf4j
@SelfType(GormEntity)
trait CatalogueItem<D extends Diffable> implements InformationAware, EditHistoryAware, Diffable<D>, CatalogueItemClassifierAware, MultiFacetAware {

    String aliasesString
    BreadcrumbTree breadcrumbTree

    void setAliases(Collection aliases) {
        String aliasString = ''
        if (aliases) {
            Collection<String> list
            if (aliases.first() instanceof Map) {
                list = aliases.collect {(it as Map).alias as String}
            } else list = aliases

            aliasString = list.collect { (it as String).trim() }?.join('|')
        }
        aliasesString = aliasString ?: null
    }

    Set<String> getAliases() {
        (aliasesString?.split(/\|/)?.findAll() ?: []) as Set
    }

    void beforeValidateCatalogueItem() {
        metadata?.each {
            it.multiFacetAwareItem = this
            if (!it.createdBy) it.createdBy = createdBy
            it.beforeValidate()
        }
        annotations?.each {
            it.multiFacetAwareItem = this
            if (!it.createdBy) it.createdBy = createdBy
            it.beforeValidate()
        }
        referenceFiles?.each {
            it.multiFacetAwareItem = this
            if (!it.createdBy) it.createdBy = createdBy
            it.beforeValidate()
        }
    }

    @Override
    String getPathIdentifier() {
        label
    }

    void updateChildIndexes(ModelItem updated, Integer oldIndex) {
        // no-op
    }

    static <T extends CatalogueItem> ObjectDiff catalogueItemDiffBuilder(Class<T> diffClass, T lhs, T rhs) {
        String lhsId = lhs.id ?: "Left:Unsaved_${lhs.domainType}"
        String rhsId = rhs.id ?: "Right:Unsaved_${rhs.domainType}"
        DiffBuilder.objectDiff(diffClass)
            .leftHandSide(lhsId, lhs)
            .rightHandSide(rhsId, rhs)
            .appendString('label', lhs.label, rhs.label)
            .appendString('description', lhs.description, rhs.description)
            .appendString('aliasesString', lhs.aliasesString, rhs.aliasesString)
            .appendList(Metadata, 'metadata', lhs.metadata, rhs.metadata)
            .appendList(Annotation, 'annotations', lhs.annotations, rhs.annotations)
            .appendList(Rule, 'rule', lhs.rules, rhs.rules)
    }

    static <T extends CatalogueItem> DetachedCriteria<T> withCatalogueItemFilter(DetachedCriteria<T> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('label', "%${filters.label}%")
        if (filters.description) criteria = criteria.ilike('description', "%${filters.description}%")
        if (filters.domainType) criteria = criteria.ilike('domainType', "%${filters.domainType}%")
        criteria
    }

    static <T extends CatalogueItem> PaginatedHibernateSearchResult<T> luceneStandardSearch(Class<T> clazz, String searchTerm, List<UUID> allowedIds,
                                                                                            Map pagination,
                                                                                            @DelegatesTo(HibernateSearchApi) Closure additional = null) {

        HibernateSearch.securedPaginatedList(clazz, allowedIds, pagination) {
            if (searchTerm) {
                simpleQueryString(searchTerm, 'label', 'description', 'aliasesString', 'metadata.key', 'metadata.value')
            }
            if (additional) {
                additional.setResolveStrategy(Closure.DELEGATE_FIRST)
                additional.setDelegate(getDelegate())
                additional.call()
            }
        }
    }

    static <T extends CatalogueItem> PaginatedHibernateSearchResult<T> luceneCustomSearch(Class<T> clazz, List<UUID> allowedIds,
                                                                                          Map pagination,
                                                                                          @DelegatesTo(HibernateSearchApi) Closure... customSearches) {
        HibernateSearch.securedPaginatedList(clazz, allowedIds, pagination, customSearches)
    }

    static <T extends CatalogueItem> PaginatedHibernateSearchResult<T> luceneLabelSearch(Class<T> clazz, String searchTerm,
                                                                                         List<UUID> allowedIds,
                                                                                         @DelegatesTo(HibernateSearchApi) Closure additional) {
        luceneLabelSearch(clazz, searchTerm, allowedIds, [:], additional)
    }

    static <T extends CatalogueItem> PaginatedHibernateSearchResult<T> luceneLabelSearch(Class<T> clazz, String searchTerm, List<UUID> allowedIds,
                                                                                         Map pagination = [:],
                                                                                         @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        HibernateSearch.securedPaginatedList(clazz, allowedIds, pagination, additional) {
            simpleQueryString searchTerm, 'label'
        }
    }
}