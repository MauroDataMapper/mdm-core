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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.CatalogueItemAwareServiceSpec

import grails.testing.services.ServiceUnitTest

import java.time.OffsetDateTime

class SummaryMetadataServiceSpec extends CatalogueItemAwareServiceSpec<SummaryMetadata, SummaryMetadataService>
    implements ServiceUnitTest<SummaryMetadataService> {

    UUID id
    DataModel dataModel

    def setup() {
        mockDomains(Folder, DataModel, Edit, SummaryMetadata, SummaryMetadataReport)
        mockArtefact(DataModelService)
        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.UNIT_TEST))
        dataModel = new DataModel(label: 'dm1', createdBy: StandardEmailAddress.UNIT_TEST, folder: Folder.findByLabel('catalogue'))
        checkAndSave(dataModel)

        dataModel.
            addToSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 1', summaryMetadataType: SummaryMetadataType.MAP)
        dataModel.addToSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 2', description: 'a description',
                                       summaryMetadataType: SummaryMetadataType.NUMBER)
        SummaryMetadata summaryMetadata = new SummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 3',
                                                              summaryMetadataType: SummaryMetadataType.STRING)
            .addToSummaryMetadataReports(createdBy: StandardEmailAddress.UNIT_TEST, reportDate: OffsetDateTime.now(), reportValue: 'some value')
        dataModel.addToSummaryMetadata(summaryMetadata)

        checkAndSave dataModel

        id = summaryMetadata.id
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<SummaryMetadata> summaryMetadata = service.list(max: 2, offset: 1)

        then:
        summaryMetadata.size() == 2

        and:
        summaryMetadata[0].label == 'summary metadata 2'
        summaryMetadata[0].description == 'a description'
        !summaryMetadata[0].summaryMetadataReports
        summaryMetadata[0].catalogueItemId == dataModel.id

        and:
        summaryMetadata[1].label == 'summary metadata 3'
        !summaryMetadata[1].description
        summaryMetadata[1].summaryMetadataReports.size() == 1
        summaryMetadata[1].catalogueItemId == dataModel.id

    }

    void "test count"() {
        expect:
        service.count() == 3
    }

    void "test delete"() {
        expect:
        service.count() == 3

        when: 'deleting'
        service.delete(id)

        then:
        service.count() == 2
    }

    @Override
    CatalogueItem getCatalogueItem() {
        dataModel
    }

    @Override
    CatalogueItem getCatalogueItemFromStorage() {
        DataModel.get(dataModel.id)
    }

    @Override
    SummaryMetadata getAwareItem() {
        SummaryMetadata.get(id)
    }

    @Override
    SummaryMetadata getUpdatedAwareItem() {
        SummaryMetadata md = SummaryMetadata.get(id)
        md.description = 'altered'
        md
    }

    @Override
    int getExpectedCountOfAwareItemsInCatalogueItem() {
        3
    }

    @Override
    String getChangedPropertyName() {
        'description'
    }
}
