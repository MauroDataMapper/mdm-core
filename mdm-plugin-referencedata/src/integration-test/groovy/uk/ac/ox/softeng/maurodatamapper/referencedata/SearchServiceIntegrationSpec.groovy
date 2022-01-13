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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.BaseReferenceDataModelIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
@Integration
@Rollback
@Tag('non-parallel')
class SearchServiceIntegrationSpec extends BaseReferenceDataModelIntegrationSpec {

    UUID referenceModelId
    UUID secondReferenceModelId
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
        log.debug('Setting up ReferenceDataModelServiceSpec unit')

        referenceModelId = buildExampleReferenceDataModel().id
        secondReferenceModelId = buildSecondExampleReferenceDataModel().id
    }

    void 'test performStandardSearch on ReferenceDataElement'() {

        given:
        setupData()

        when:
        SearchParams searchParams = new SearchParams(search: 'Organisation')
        List<ModelItem> modelItems = searchService.performStandardSearch([ReferenceDataElement].toSet(), [referenceModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 2

        when:
        searchParams = new SearchParams(search: 'nothing')
        modelItems = searchService.performStandardSearch([ReferenceDataElement].toSet(), [referenceModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
    }

    void 'test perform x search on simple DataModel looking for metadata entry'() {

        given:
        setupData()

        when: 'standard search'
        SearchParams searchParams = new SearchParams(search: 'mdk1')
        List<ModelItem> modelItems = searchService.performStandardSearch([ReferenceDataModel].toSet(), [referenceModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 1

        when: 'label only search'
        modelItems = searchService.performLabelSearch([ReferenceDataModel].toSet(), [referenceModelId], searchParams.searchTerm)

        then:
        modelItems.size() == 0
    }
}
