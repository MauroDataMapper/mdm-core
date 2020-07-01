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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.report

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.functional.CatalogueItemSummaryMetadataReportFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReportController
 */
@Integration
@Slf4j
class DataElementSummaryMetadataReportFunctionalSpec extends CatalogueItemSummaryMetadataReportFunctionalSpec {

    @Shared
    DataModel dataModel
    @Shared
    DataClass dataClass
    @Shared
    DataElement dataElement
    @Shared
    DataType dataType

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        assert Folder.count() == 0
        assert DataModel.count() == 0
        Folder folder = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true)
        dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                  folder: folder).save(flush: true)
        dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                  dataModel: dataModel).save(flush: true)
        dataType = new PrimitiveType(label: 'string', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                     dataModel: dataModel).save(flush: true)
        dataElement = new DataElement(label: 'Functional Test DataElement', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                      dataModel: dataModel, dataClass: dataClass, dataType: dataType).save(flush: true)
        summaryMetadata = new SummaryMetadata(label: 'Functional Test Summary Metadata', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                              catalogueItem: dataElement, summaryMetadataType: SummaryMetadataType.NUMBER).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PluginCatalogueItemFunctionalSpec')
        cleanUpResources(DataModel, Folder, DataClass, DataElement, DataType)
    }

    @Override
    UUID getCatalogueItemId() {
        dataElement.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'dataElements'
    }
}