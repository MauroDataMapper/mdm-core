/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.metadata

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemMetadataFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * Where facet owner is a ReferenceDataType
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController
 */
@Integration
@Slf4j
class ReferenceDataTypeMetadataFunctionalSpec extends CatalogueItemMetadataFunctionalSpec {

    @Shared
    ReferenceDataModel referenceDataModel
    @Shared
    ReferenceDataModel destinationReferenceDataModel
    @Shared
    ReferenceDataElement referenceDataElement
    @Shared
    ReferenceDataType referenceDataType

    String getCatalogueItemCopyPath() {
        "referenceDataModels/${destinationDataModelId}/${catalogueItemDomainResourcePath}/${sourceDataModelId}/${catalogueItemId}"
    }

    @Transactional
    String getSourceDataModelId() {
        ReferenceDataModel.findByLabel('Functional Test ReferenceDataModel').id.toString()
    }

    @Transactional
    String getDestinationDataModelId() {
        ReferenceDataModel.findByLabel('Destination Test ReferenceDataModel').id.toString()
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        referenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel', createdBy: 'functionalTest@test.com',
                                  folder: folder, authority: testAuthority).save(flush: true)
        destinationReferenceDataModel = new ReferenceDataModel(label: 'Destination Test ReferenceDataModel', createdBy: 'functionalTest@test.com',
                                             folder: folder, authority: testAuthority).save(flush: true)
        referenceDataType = new ReferencePrimitiveType(label: 'string', createdBy: 'functionalTest@test.com',
                                     referenceDataModel: referenceDataModel).save(flush: true)
        referenceDataElement = new ReferenceDataElement(label: 'Functional Test ReferenceDataElement', createdBy: 'functionalTest@test.com',
                                      referenceDataModel: referenceDataModel, referenceDataType: referenceDataType).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(ReferenceDataModel, Folder, ReferenceDataElement, ReferenceDataType)
    }

    @Override
    UUID getCatalogueItemId() {
        referenceDataType.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'referenceDataTypes'
    }
}