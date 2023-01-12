/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.search.AbstractCatalogueItemSearchService
import uk.ac.ox.softeng.maurodatamapper.core.search.CatalogueItemSearchDomainProvider
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult

class SearchService extends AbstractCatalogueItemSearchService<ModelItem> implements CatalogueItemSearchDomainProvider {

    PaginatedHibernateSearchResult<ModelItem> findAllByDataModelIdByHibernateSearch(UUID dataModelId, SearchParams searchParams,
                                                                                    Map pagination = [:]) {
        findAllCatalogueItemsOfTypeByOwningIdsByHibernateSearch([dataModelId], searchParams, true, pagination)
    }

    PaginatedHibernateSearchResult<ModelItem> findAllByDataClassIdByHibernateSearch(UUID dataClassId, SearchParams searchParams,
                                                                                    Map pagination = [:]) {
        findAllCatalogueItemsOfTypeByOwningIdsByHibernateSearch([dataClassId], searchParams, true, pagination)
    }

    @Override
    Set<Class<ModelItem>> getDomainsToSearch() {
        (getSearchableCatalogueItemDomains() - [DataModel]) as HashSet<Class<ModelItem>>
    }

    @Override
    Set<Class<CatalogueItem>> getSearchableCatalogueItemDomains() {
        [DataModel, DataClass, DataElement, ReferenceType, EnumerationType, PrimitiveType, EnumerationValue] as HashSet<Class<CatalogueItem>>
    }
}
