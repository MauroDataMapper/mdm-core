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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.model.container.CatalogueItemClassifierAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.HibernateSearch
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 06/12/2019
 */
@SelfType(GormEntity)
trait CatalogueItem<D extends Diffable> implements MdmDomain, InformationAware, EditHistoryAware, Diffable<D>, CatalogueItemClassifierAware, MultiFacetAware {

    String aliasesString
    BreadcrumbTree breadcrumbTree

    void setAliases(Collection aliases) {
        String aliasString = ''
        if (aliases) {
            Collection<String> list
            if (aliases.first() instanceof Map) {
                list = aliases.collect {(it as Map).alias as String}
            } else list = aliases

            aliasString = list.collect {(it as String).trim()}?.join('|')
        }
        aliasesString = aliasString ?: null
    }

    Set<String> getAliases() {
        (aliasesString?.split(/\|/)?.findAll() ?: []) as Set
    }

    void beforeValidateCatalogueItem() {
        checkPath() // get path to ensure its built
        if (breadcrumbTree) {
            // Dont need to "recheck" the path as we've just done that
            if (!breadcrumbTree.matchesPath(getUncheckedPath())) {
                breadcrumbTree.update(this)
            }
        } else {
            breadcrumbTree = new BreadcrumbTree(this)
        }
        metadata?.each {
            it.beforeValidateCheck(this)
        }
        annotations?.each {
            it.beforeValidateCheck(this)
        }
        referenceFiles?.each {
            it.beforeValidateCheck(this)
        }
        rules?.each {
            it.beforeValidateCheck(this)
        }
    }

    @Override
    String getPathIdentifier() {
        label
    }

    void updateChildIndexes(ModelItem updated, Integer oldIndex) {
        // no-op
    }

    static <T extends CatalogueItem> DetachedCriteria<T> withCatalogueItemFilter(DetachedCriteria<T> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('label', "%${filters.label}%")
        if (filters.description) criteria = criteria.ilike('description', "%${filters.description}%")
        if (filters.domainType) criteria = criteria.ilike('domainType', "%${filters.domainType}%")
        criteria
    }

    static <T extends CatalogueItem> PaginatedHibernateSearchResult<T> standardHibernateSearch(Class<T> clazz, String searchTerm,
                                                                                               List<UUID> allowedIds,
                                                                                               List<Path> allowedPaths, Map pagination,
                                                                                               @DelegatesTo(HibernateSearchApi)
                                                                                                   Closure additional = null) {

        HibernateSearch.securedPaginatedList(clazz, allowedIds, allowedPaths, pagination) {
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

    static <T extends CatalogueItem> PaginatedHibernateSearchResult<T> customHibernateSearch(Class<T> clazz, List<UUID> allowedIds,
                                                                                             List<Path> allowedPaths, Map pagination,
                                                                                             @DelegatesTo(HibernateSearchApi)
                                                                                                 Closure... customSearches) {
        HibernateSearch.securedPaginatedList(clazz, allowedIds, allowedPaths, pagination, customSearches)
    }

    static <T extends CatalogueItem> PaginatedHibernateSearchResult<T> labelHibernateSearch(Class<T> clazz, String searchTerm,
                                                                                            List<UUID> allowedIds, List<Path> allowedPaths,
                                                                                            @DelegatesTo(HibernateSearchApi) Closure additional) {
        labelHibernateSearch(clazz, searchTerm, allowedIds, allowedPaths, [:], additional)
    }

    static <T extends CatalogueItem> PaginatedHibernateSearchResult<T> labelHibernateSearch(Class<T> clazz, String searchTerm, List<UUID> allowedIds,
                                                                                            List<Path> allowedPaths, Map pagination = [:],
                                                                                            @DelegatesTo(HibernateSearchApi)
                                                                                                Closure additional = null) {
        HibernateSearch.securedPaginatedList(clazz, allowedIds, allowedPaths, pagination, additional) {
            simpleQueryString searchTerm, 'label'
        }
    }
}