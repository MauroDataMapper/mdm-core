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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.ReferenceSummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

@Slf4j
class ReferenceSummaryMetadataControllerSpec extends ResourceControllerSpec<ReferenceSummaryMetadata> implements
    DomainUnitTest<ReferenceSummaryMetadata>,
    ControllerUnitTest<ReferenceSummaryMetadataController> {

    ReferenceDataModel referenceDataModel

    def setup() {
        mockDomains(Folder, ReferenceDataModel, ReferenceSummaryMetadata, ReferenceSummaryMetadataReport)
        log.debug('Setting up reference summary metadata controller unit')
        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.UNIT_TEST))
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost')
        checkAndSave(testAuthority)
        referenceDataModel = new ReferenceDataModel(label: 'dm1', createdBy: StandardEmailAddress.UNIT_TEST, folder: Folder.findByLabel('catalogue'), authority: testAuthority)
        checkAndSave referenceDataModel

        referenceDataModel.
            addToReferenceSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 1',
                                          summaryMetadataType: ReferenceSummaryMetadataType.MAP)

        referenceDataModel.addToReferenceSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 2',
                                                         description: 'a description',
                                                         summaryMetadataType: ReferenceSummaryMetadataType.NUMBER)
        domain.createdBy = StandardEmailAddress.UNIT_TEST
        domain.label = 'summary metadata 3'
        domain.summaryMetadataType = ReferenceSummaryMetadataType.STRING
        domain.multiFacetAwareItem = referenceDataModel
        domain.addToSummaryMetadataReports(createdBy: StandardEmailAddress.UNIT_TEST, reportDate: OffsetDateTime.now(), reportValue: 'some value')
        referenceDataModel.addToReferenceSummaryMetadata(domain)

        checkAndSave(referenceDataModel)

        controller.referenceSummaryMetadataService = Mock(ReferenceSummaryMetadataService) {
            findAllByMultiFacetAwareItemId(referenceDataModel.id, _) >> referenceDataModel.referenceSummaryMetadata.toList()
            findMultiFacetAwareItemByDomainTypeAndId(ReferenceDataModel.simpleName, _) >>
            {String domain, UUID bid -> referenceDataModel.id == bid ? referenceDataModel : null}
            findByMultiFacetAwareItemIdAndId(_, _) >> {UUID iid, Serializable mid ->
                if (iid != referenceDataModel.id) return null
                mid == domain.id ? domain : null
            }
            addFacetToDomain(_, _, _) >> {ReferenceSummaryMetadata md, String domain, UUID bid ->
                if (referenceDataModel.id == bid) {
                    referenceDataModel.addToReferenceSummaryMetadata(md)
                    md.multiFacetAwareItem = referenceDataModel
                }
            }
        }
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 3,
  "items": [
   {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "summaryMetadataType": "MAP",
      "createdBy": "unit-test@test.com",
      "id": "${json-unit.matches:id}",
      "label": "summary metadata 1"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "summaryMetadataType": "NUMBER",
      "createdBy": "unit-test@test.com",
      "description": "a description",
      "id": "${json-unit.matches:id}",
      "label": "summary metadata 2"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "summaryMetadataType": "STRING",
      "createdBy": "unit-test@test.com",
      "id": "${json-unit.matches:id}",
      "label": "summary metadata 3"
    }
  ]
}
'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '''{
  "total": 2,
  "errors": [
    {"message": "Property [label] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata] cannot be null"},
    {"message": "Property [summaryMetadataType] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [summaryMetadataType] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "summaryMetadataType": "NUMBER",
  "createdBy": "unlogged_user@mdm-core.com",
  "description": "a description",
  "id": "${json-unit.matches:id}",
  "label": "valid"
}'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "summaryMetadataType": "STRING",
  "createdBy": "unit-test@test.com",
  "id": "${json-unit.matches:id}",
  "label": "summary metadata 3"
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [label] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "summaryMetadataType": "STRING",
  "createdBy": "unit-test@test.com",
  "description": "added an updated",
  "id": "${json-unit.matches:id}",
  "label": "summary metadata 3"
}'''
    }

    @Override
    ReferenceSummaryMetadata invalidUpdate(ReferenceSummaryMetadata instance) {
        instance.label = null
        instance
    }

    @Override
    ReferenceSummaryMetadata validUpdate(ReferenceSummaryMetadata instance) {
        instance.description = 'added an updated'
        instance
    }

    @Override
    ReferenceSummaryMetadata getInvalidUnsavedInstance() {
        new ReferenceSummaryMetadata(label: 'invalid',)
    }

    @Override
    ReferenceSummaryMetadata getValidUnsavedInstance() {
        new ReferenceSummaryMetadata(label: 'valid', description: 'a description', summaryMetadataType: ReferenceSummaryMetadataType.NUMBER)
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.multiFacetAwareItemDomainType = ReferenceDataModel.simpleName
        params.multiFacetAwareItemId = referenceDataModel.id
    }
}