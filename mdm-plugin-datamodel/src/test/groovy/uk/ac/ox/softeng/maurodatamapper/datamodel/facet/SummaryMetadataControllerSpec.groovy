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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataController
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

@Slf4j
class SummaryMetadataControllerSpec extends ResourceControllerSpec<SummaryMetadata> implements
    DomainUnitTest<SummaryMetadata>,
    ControllerUnitTest<SummaryMetadataController> {

    DataModel dataModel

    def setup() {
        mockDomains(Folder, DataModel, SummaryMetadata, SummaryMetadataReport)
        log.debug('Setting up summary metadata controller unit')
        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.UNIT_TEST))
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost')
        checkAndSave(testAuthority)
        dataModel = new DataModel(label: 'dm1', createdBy: StandardEmailAddress.UNIT_TEST, folder: Folder.findByLabel('catalogue'), authority: testAuthority)
        checkAndSave dataModel

        dataModel.
            addToSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 1', summaryMetadataType: SummaryMetadataType.MAP)
        dataModel.addToSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 2', description: 'a description',
                                       summaryMetadataType: SummaryMetadataType.NUMBER)
        domain.createdBy = StandardEmailAddress.UNIT_TEST
        domain.label = 'summary metadata 3'
        domain.summaryMetadataType = SummaryMetadataType.STRING
        domain.multiFacetAwareItem = dataModel
        domain.addToSummaryMetadataReports(createdBy: StandardEmailAddress.UNIT_TEST, reportDate: OffsetDateTime.now(), reportValue: 'some value')
        dataModel.addToSummaryMetadata(domain)

        checkAndSave(dataModel)

        controller.summaryMetadataService = Mock(SummaryMetadataService) {
            findAllByMultiFacetAwareItemId(dataModel.id, _) >> dataModel.summaryMetadata.toList()
            findMultiFacetAwareItemByDomainTypeAndId(DataModel.simpleName, _) >> {String domain, UUID bid -> dataModel.id == bid ? dataModel : null}
            findByMultiFacetAwareItemIdAndId(_, _) >> {UUID iid, Serializable mid ->
                if (iid != dataModel.id) return null
                mid == domain.id ? domain : null
            }
            addFacetToDomain(_, _, _) >> {SummaryMetadata md, String domain, UUID bid ->
                if (dataModel.id == bid) {
                    dataModel.addToSummaryMetadata(md)
                    md.multiFacetAwareItem = dataModel
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
    {"message": "Property [label] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata] cannot be null"},
    {"message": "Property [summaryMetadataType] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [summaryMetadataType] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata] cannot be null"}
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
    {"message": "Property [label] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata] cannot be null"}
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
    SummaryMetadata invalidUpdate(SummaryMetadata instance) {
        instance.label = null
        instance
    }

    @Override
    SummaryMetadata validUpdate(SummaryMetadata instance) {
        instance.description = 'added an updated'
        instance
    }

    @Override
    SummaryMetadata getInvalidUnsavedInstance() {
        new SummaryMetadata(label: 'invalid',)
    }

    @Override
    SummaryMetadata getValidUnsavedInstance() {
        new SummaryMetadata(label: 'valid', description: 'a description', summaryMetadataType: SummaryMetadataType.NUMBER)
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.multiFacetAwareItemDomainType = DataModel.simpleName
        params.multiFacetAwareItemId = dataModel.id
    }
}