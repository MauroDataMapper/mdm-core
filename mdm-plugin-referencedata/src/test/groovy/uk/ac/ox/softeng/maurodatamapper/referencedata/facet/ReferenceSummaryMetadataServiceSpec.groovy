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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.ReferenceSummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.CatalogueItemAwareServiceSpec

import grails.testing.services.ServiceUnitTest

import java.time.OffsetDateTime

class ReferenceSummaryMetadataServiceSpec extends CatalogueItemAwareServiceSpec<ReferenceSummaryMetadata, ReferenceSummaryMetadataService>
    implements ServiceUnitTest<ReferenceSummaryMetadataService> {

    UUID id
    ReferenceDataModel referenceDataModel

    def setup() {
        mockArtefact(ClassifierService)
        mockArtefact(VersionLinkService)
        mockArtefact(SemanticLinkService)
        mockArtefact(EditService)
        mockArtefact(MetadataService)
        mockArtefact(ReferenceDataTypeService)
        mockDomains(Folder, ReferenceDataModel, Edit, ReferenceSummaryMetadata, ReferenceSummaryMetadataReport, Authority, Metadata, VersionLink, SemanticLink, Classifier)
        mockArtefact(ReferenceDataModelService)
        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.UNIT_TEST))
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.UNIT_TEST))
        referenceDataModel = new ReferenceDataModel(label: 'dm1', createdBy: StandardEmailAddress.UNIT_TEST, folder: Folder.findByLabel('catalogue'),
                                  authority: Authority.findByLabel('Test Authority'))
        checkAndSave(referenceDataModel)

        referenceDataModel.
            addToReferenceSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 1',
                                          summaryMetadataType: ReferenceSummaryMetadataType.MAP)

        referenceDataModel.
            addToReferenceSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 2', description: 'a description',
                                          summaryMetadataType: ReferenceSummaryMetadataType.NUMBER)

        ReferenceSummaryMetadata referenceSummaryMetadata = new ReferenceSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST,
                                                                                         label: 'summary metadata 3',
                                                                                         summaryMetadataType: ReferenceSummaryMetadataType.STRING)
            .addToSummaryMetadataReports(createdBy: StandardEmailAddress.UNIT_TEST, reportDate: OffsetDateTime.now(), reportValue: 'some value')

        referenceDataModel.addToReferenceSummaryMetadata(referenceSummaryMetadata)

        checkAndSave referenceDataModel

        ReferenceDataModelService dataModelService = Stub() {
            get(_) >> referenceDataModel
            getModelClass() >> referenceDataModel
            handles('ReferenceDataModel') >> true
            removeReferenceSummaryMetadataFromCatalogueItem(referenceDataModel.id, _) >> {UUID bmid, ReferenceSummaryMetadata sm ->
                referenceDataModel.referenceSummaryMetadata.remove(sm)
            }
        }
        service.catalogueItemServices = [dataModelService]

        id = referenceSummaryMetadata.id
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<ReferenceSummaryMetadata> summaryMetadata = service.list(max: 2, offset: 1)

        then:
        summaryMetadata.size() == 2

        and:
        summaryMetadata[0].label == 'summary metadata 2'
        summaryMetadata[0].description == 'a description'
        !summaryMetadata[0].summaryMetadataReports
        summaryMetadata[0].catalogueItemId == referenceDataModel.id

        and:
        summaryMetadata[1].label == 'summary metadata 3'
        !summaryMetadata[1].description
        summaryMetadata[1].summaryMetadataReports.size() == 1
        summaryMetadata[1].catalogueItemId == referenceDataModel.id

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
        referenceDataModel
    }

    @Override
    CatalogueItem getCatalogueItemFromStorage() {
        ReferenceDataModel.get(referenceDataModel.id)
    }

    @Override
    ReferenceSummaryMetadata getAwareItem() {
        ReferenceSummaryMetadata.get(id)
    }

    @Override
    ReferenceSummaryMetadata getUpdatedAwareItem() {
        ReferenceSummaryMetadata md = ReferenceSummaryMetadata.get(id)
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
