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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
@Integration
@Rollback
class SearchServiceIntegrationSpec extends BaseDataModelIntegrationSpec {

    UUID complexDataModelId
    UUID simpleDataModelId
    @Autowired
    SearchService mdmPluginDataModelSearchService
    ApplicationContext applicationContext

    SearchService getSearchService() {
        mdmPluginDataModelSearchService
    }

    @Override
    void preDomainDataSetup() {
        super.preDomainDataSetup()
        hibernateSearchIndexingService.purgeAllIndexes()
    }

    @Override
    void postDomainDataSetup() {
        hibernateSearchIndexingService.flushIndexes()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up DataModelServiceSpec unit')

        complexDataModelId = buildComplexDataModel().id
        simpleDataModelId = buildSimpleDataModel().id
    }

    void 'S1 - test performStandardSearch on simple DataModel'() {

        given:
        setupData()

        when:
        SearchParams searchParams = new SearchParams(search: 'simple')
        List<ModelItem> modelItems = searchService.performStandardSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 1

        when:
        searchParams = new SearchParams(search: 'nothing')
        modelItems = searchService.performStandardSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
    }

    void 'S2 - test performLabelSearch on simple DataModel'() {

        given:
        setupData()

        when:
        SearchParams searchParams = new SearchParams(search: 'simple')
        List<ModelItem> modelItems = searchService.performLabelSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 1

        when:
        searchParams = new SearchParams(search: 'nothing')
        modelItems = searchService.performLabelSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
    }

    void 'S3 - test performStandardSearch domain restricted on simple DataModel'() {

        given:
        setupData()

        when:
        SearchParams searchParams = new SearchParams(search: 'simple')
        List<ModelItem> modelItems = searchService.performStandardSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 1

        when:
        searchParams = new SearchParams(search: 'nothing')
        modelItems = searchService.performStandardSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
        when:
        searchParams = new SearchParams(search: 'simple')
        modelItems = searchService.performStandardSearch([DataElement].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
        when:
        searchParams = new SearchParams(search: 'simple', domainType: '')
        modelItems = searchService.performStandardSearch([ReferenceType].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
        when:
        searchParams = new SearchParams(search: 'simple')
        modelItems = searchService.performStandardSearch([DataClass, PrimitiveType].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 1
    }

    void 'S4 - test performLabelSearch  domain restricted label search on simple DataModel'() {

        given:
        setupData()

        when:
        SearchParams searchParams = new SearchParams(search: 'simple')
        List<ModelItem> modelItems = searchService.performLabelSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 1

        when:
        searchParams = new SearchParams(search: 'simple')
        modelItems = searchService.performLabelSearch([DataClass, DataElement].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 1

        when:
        searchParams = new SearchParams(search: 'simple')
        modelItems = searchService.performLabelSearch([DataElement].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0

        when:
        searchParams = new SearchParams(search: 'simple')
        modelItems = searchService.performLabelSearch([ReferenceType].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0

        when:
        searchParams = new SearchParams(search: 'simple')
        modelItems = searchService.performLabelSearch([DataElement, PrimitiveType].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
    }

    void 'S5 - test perform x search on simple DataModel looking for metadata entry'() {

        given:
        setupData()

        when: 'standard search'
        SearchParams searchParams = new SearchParams(search: 'mdk1')
        List<ModelItem> modelItems = searchService.performStandardSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 1

        when: 'label only search'
        modelItems = searchService.performLabelSearch([DataClass].toSet(), [simpleDataModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
    }

    void 'S6 - test findAllByDataModelIdByLuceneSearch on complex DataModel with no pagination'() {

        given:
        setupData()
        SearchParams searchParams = new SearchParams(search: 'emptyclass')

        when:
        PaginatedHibernateSearchResult<ModelItem> result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams)

        then:
        result.count == 1
        result.results[0].label == 'emptyclass'
        result.results[0].domainType == 'DataClass'

        when:
        searchParams = new SearchParams(search: 'string')
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams)

        then:
        result.count == 1
        result.results[0].label == 'string'
        result.results[0].domainType == 'PrimitiveType'

        when:
        searchParams = new SearchParams(search: 'ele*')
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams)

        then:
        result.count == 3
        result.results.any { it.label == 'ele1' && it.domainType == 'DataElement' }
        result.results.any { it.label == 'element2' && it.domainType == 'DataElement' }
        result.results.any { it.label == 'content' && it.domainType == 'DataClass' && it.description == 'A dataclass with elements' }

        when:
        searchParams = new SearchParams(search: 'child')
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams)

        then:
        result.count == 3
        result.results.any { it.label == 'child' && it.domainType == 'DataClass' }
        result.results.any { it.label == 'child' && it.domainType == 'DataElement' }
        result.results.any { it.label == 'child' && it.domainType == 'ReferenceType' }

        when:
        searchParams = new SearchParams(search: 'yes')
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams)

        then:
        result.count == 1
        result.results[0].label == 'Y'
        result.results[0].domainType == 'EnumerationValue'
    }

    void 'S7 - test findAllByDataModelIdByLuceneSearch on complex DataModel with pagination'() {

        given:
        setupData()
        SearchParams searchParams = new SearchParams(search: 'ele*')
        Map pagination = [sort: 'label']


        when:
        PaginatedHibernateSearchResult<ModelItem> result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 3
        result.results[0].label == 'content'
        result.results[1].label == 'ele1'
        result.results[2].label == 'element2'

        when:
        pagination.order = 'desc'
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 3
        result.results[2].label == 'content'
        result.results[1].label == 'ele1'
        result.results[0].label == 'element2'

        when:
        pagination = [max: 2]
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 3
        result.results[0].label == 'content'
        result.results[1].label == 'ele1'

        when:
        pagination = [max: 2, offset: 1]
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 3
        result.results[0].label == 'ele1'
        result.results[1].label == 'element2'
    }

    void 'S8 - test findAllByDataModelIdByLuceneSearch on complex DataModel with domain filtering'() {

        given:
        setupData()
        SearchParams searchParams = new SearchParams(search: 'ele*', domainTypes: [DataClass.simpleName])
        Map pagination = [sort: 'label']


        when:
        PaginatedHibernateSearchResult<ModelItem> result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 1
        result.results[0].label == 'content'

        when:
        searchParams.domainTypes = [DataElement.simpleName]
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 2
        result.results[0].label == 'ele1'
        result.results[1].label == 'element2'

        when:
        searchParams.searchTerm = 'child'
        searchParams.domainTypes = [DataElement.simpleName]
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 1
        result.results.any { it.label == 'child' && it.domainType == 'DataElement' }

        when:
        searchParams.domainTypes = [DataElement.simpleName, DataClass.simpleName]
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 2
        result.results.any { it.label == 'child' && it.domainType == 'DataClass' }
        result.results.any { it.label == 'child' && it.domainType == 'DataElement' }

        when:
        searchParams.domainTypes = [ReferenceType.simpleName]
        result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 1
        result.results.any { it.label == 'child' && it.domainType == 'ReferenceType' }
    }

    void 'S9 - test findAllByDataModelIdByLuceneSearch on complex DataModel label search only'() {

        given:
        setupData()
        SearchParams searchParams = new SearchParams(search: 'ele*', labelOnly: true)
        Map pagination = [sort: 'label']


        when:
        PaginatedHibernateSearchResult<ModelItem> result = searchService.findAllByDataModelIdByLuceneSearch(complexDataModelId, searchParams, pagination)

        then:
        result.count == 2
        result.results[0].label == 'ele1'
        result.results[1].label == 'element2'
    }
}
