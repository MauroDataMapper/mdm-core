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
/*package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.functional.CatalogueItemReferenceSummaryMetadataFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared*/

/**
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataController
 */
/*@Integration
@Slf4j
class DataModelReferenceSummaryMetadataFunctionalSpec extends CatalogueItemReferenceSummaryMetadataFunctionalSpec {

    @Shared
    DataModel dataModel
    @Shared
    DataClass dataClass
    @Shared
    ReferenceDataElement dataElement
    @Shared
    ReferenceDataType dataType

    String getCatalogueItemCopyPath() {
        "dataModels/${sourceDataModelId}/newForkModel"
    }

    @Transactional
    String getSourceDataModelId() {
        DataModel.findByLabel('Functional Test DataModel').id.toString()
    }

    @Transactional
    String getDestinationDataModelId() {
        // newForkModel doesn't require a destination data model
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                  folder: folder, authority: testAuthority).save(flush: true)
        dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                  dataModel: dataModel).save(flush: true)
        dataType = new ReferencePrimitiveType(label: 'string', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                     dataModel: dataModel).save(flush: true)
        dataElement = new ReferenceDataElement(label: 'Functional Test DataElement', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                      dataModel: dataModel, dataClass: dataClass, referenceDataType: dataType).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PluginCatalogueItemFunctionalSpec')
        cleanUpResources(DataModel, Folder, DataClass, ReferenceDataElement, ReferenceDataType)
    }

    @Override
    UUID getCatalogueItemId() {
        dataModel.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'dataModels'
    }

    @Override
    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        // Summary metadata only copied for new doc version
    }

    @Override
    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        // Summary metadata only copied for new doc version
    }

    @Override
    void verifyCIF01CopiedFacetSuccessfully(HttpResponse response) {
        // Summary metadata only copied for new doc version
    }
}*/