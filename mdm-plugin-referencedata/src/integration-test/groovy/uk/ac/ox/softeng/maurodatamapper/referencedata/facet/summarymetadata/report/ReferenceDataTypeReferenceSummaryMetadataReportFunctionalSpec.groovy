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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.report

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.functional.CatalogueItemReferenceSummaryMetadataReportFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.ReferenceSummaryMetadataReportController
 */
@Integration
@Slf4j
class ReferenceDataTypeReferenceSummaryMetadataReportFunctionalSpec extends CatalogueItemReferenceSummaryMetadataReportFunctionalSpec {

    @Shared
    ReferenceDataModel referenceDataModel
    @Shared
    ReferenceDataModel destinationReferenceDataModel
    @Shared
    ReferenceDataElement referenceDataElement
    @Shared
    ReferenceDataType referenceDataType

    String getCatalogueItemCopyPath() {
        """referenceDataModels/${destinationDataModelId}/${catalogueItemDomainResourcePath}/${sourceDataModelId}/${catalogueItemId}"""
    }

    @Transactional
    String getSourceDataModelId() {
        ReferenceDataModel.findByLabel('Functional Test DataModel').id.toString()
    }

    @Transactional
    String getDestinationDataModelId() {
        ReferenceDataModel.findByLabel('Destination Test DataModel').id.toString()
    }

    @Override
    String getFacetResourcePath() {
        "referenceSummaryMetadata/${summaryMetadata.id}/summaryMetadataReports"
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        referenceDataModel = new ReferenceDataModel(label: 'Functional Test DataModel', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                  folder: folder, authority: testAuthority).save(flush: true)
        destinationReferenceDataModel = new ReferenceDataModel(label: 'Destination Test DataModel', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                             folder: folder, authority: testAuthority).save(flush: true)

        referenceDataType = new ReferencePrimitiveType(label: 'string', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                     referenceDataModel: referenceDataModel).save(flush: true)
        referenceDataElement = new ReferenceDataElement(label: 'Functional Test DataElement', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                      referenceDataModel: referenceDataModel, referenceDataType: referenceDataType).save(flush: true)
        summaryMetadata = new ReferenceSummaryMetadata(label: 'Functional Test Summary Metadata', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                                       multiFacetAwareItem: referenceDataElement,
                                                       summaryMetadataType: ReferenceSummaryMetadataType.NUMBER).save(flush: true)
        sessionFactory.currentSession.flush()
    }


    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PluginCatalogueItemFunctionalSpec')
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