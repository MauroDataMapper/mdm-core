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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.rule

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemRuleFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController
 */
@Integration
@Slf4j
class DataClassRuleFunctionalSpec extends CatalogueItemRuleFunctionalSpec {

    @Shared
    DataModel dataModel
    @Shared
    DataModel destinationDataModel
    @Shared
    DataClass dataClass
    @Shared
    DataElement dataElement
    @Shared
    DataType dataType

    @Transactional
    String getSourceCatalogueItemId() {
        DataModel.findByLabel('Functional Test DataModel').id.toString()
    }

    @Transactional
    String getDestinationCatalogueItemId() {
        DataModel.findByLabel('Destination Test DataModel').id.toString()
    }

    String getCatalogueItemCopyPath() {
        "dataModels/${destinationCatalogueItemId}/${catalogueItemDomainResourcePath}/${sourceCatalogueItemId}/${catalogueItemId}"
    }    

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: 'functionalTest@test.com',
                                  folder: folder, authority: testAuthority).save(flush: true)
        destinationDataModel = new DataModel(label: 'Destination Test DataModel', createdBy: 'functionalTest@test.com',
                                             folder: folder, authority: testAuthority).save(flush: true)
        dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: 'functionalTest@test.com',
                                  dataModel: dataModel).save(flush: true)
        dataType = new PrimitiveType(label: 'string', createdBy: 'functionalTest@test.com',
                                     dataModel: dataModel).save(flush: true)
        dataElement = new DataElement(label: 'Functional Test DataElement', createdBy: 'functionalTest@test.com',
                                      dataModel: dataModel, dataClass: dataClass, dataType: dataType).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(DataModel, Folder, DataClass, DataElement, DataType)
    }

    @Override
    UUID getCatalogueItemId() {
        dataClass.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'dataClasses'
    }

    //TODO - check
    @Override
    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        // Rule only copied for new doc version
    }

    //TODO - check
    @Override
    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        /// Rule only copied for new doc version
    }

    //TODO - check
    @Override
    void verifyCIF01CopiedFacetSuccessfully(HttpResponse response) {
        // Rule only copied for new doc version
    }
}