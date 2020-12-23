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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime
import java.time.temporal.ChronoField

@Slf4j
class SummaryMetadataReportControllerSpec extends ResourceControllerSpec<SummaryMetadataReport> implements
    DomainUnitTest<SummaryMetadataReport>,
    ControllerUnitTest<SummaryMetadataReportController> {

    DataModel dataModel
    OffsetDateTime dateTime
    SummaryMetadata summaryMetadata

    def setup() {
        mockDomains(Folder, DataModel, SummaryMetadata, SummaryMetadataReport, Authority)
        log.debug('Setting up summary metadata controller unit')
        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.UNIT_TEST))
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.UNIT_TEST))
        dataModel = new DataModel(label: 'dm1', createdBy: StandardEmailAddress.UNIT_TEST, folder: Folder.findByLabel('catalogue'),
                                  authority: Authority.findByLabel('Test Authority'))
        checkAndSave dataModel
        // Need to make sure this never gets set to anything which is less than 3 digits when formatted as then the test fails
        dateTime = OffsetDateTime.now().with(ChronoField.MILLI_OF_SECOND, 414)

        summaryMetadata = new SummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 3',
                                              summaryMetadataType: SummaryMetadataType.STRING)
        domain.createdBy = StandardEmailAddress.UNIT_TEST
        domain.reportValue = 'a report value'
        domain.reportDate = dateTime
        summaryMetadata.addToSummaryMetadataReports(domain)
            .addToSummaryMetadataReports(createdBy: StandardEmailAddress.UNIT_TEST,
                                         reportValue: 'another report',
                                         reportDate: dateTime.plusDays(1))
            .addToSummaryMetadataReports(createdBy: StandardEmailAddress.UNIT_TEST,
                                         reportValue: 'another report 2',
                                         reportDate: dateTime.plusDays(2))

        dataModel.addToSummaryMetadata(summaryMetadata)

        checkAndSave(dataModel)

        controller.summaryMetadataReportService = Stub(SummaryMetadataReportService) {
            findBySummaryMetadataIdAndId(summaryMetadata.id, _) >> {UUID iid, Serializable mid ->
                if (iid != summaryMetadata.id) return null
                mid == domain.id ? domain : null
            }
            findAllBySummaryMetadataId(summaryMetadata.id, _) >> summaryMetadata.summaryMetadataReports.toList()
            findCatalogueItemByDomainTypeAndId(DataModel.simpleName, _) >> {String domain, UUID bid -> dataModel.id == bid ? dataModel : null}
        }
        controller.summaryMetadataService = Stub(SummaryMetadataService) {
            get(summaryMetadata.id) >> summaryMetadata
        }
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 3,
  "items": [
  {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "reportDate": "${json-unit.matches:offsetDateTime}",
      "id": "${json-unit.matches:id}",
      "reportValue": "a report value"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "reportDate": "${json-unit.matches:offsetDateTime}",
      "id": "${json-unit.matches:id}",
      "reportValue": "another report"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "reportDate": "${json-unit.matches:offsetDateTime}",
      "id": "${json-unit.matches:id}",
      "reportValue": "another report 2"
    }
  ]
}RestResponder
'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '''{
  "total": 2,
  "errors": [
    {
      "message": "Property [reportDate] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata''' +
        '''.SummaryMetadataReport] cannot be null"
    },
    {
      "message": "Property [reportValue] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata''' +
        '''.SummaryMetadataReport] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 1,
  "errors": [
{"message": "Property [reportDate] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.''' +
        '''SummaryMetadataReport] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "reportDate": "${json-unit.matches:offsetDateTime}",
  "id": "${json-unit.matches:id}",
  "reportValue": "valid"
}'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "reportDate": "${json-unit.matches:offsetDateTime}",
  "id": "${json-unit.matches:id}",
  "reportValue": "a report value"
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [reportValue] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.''' +
        '''SummaryMetadataReport] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "reportDate": "${json-unit.matches:offsetDateTime}",
  "id": "${json-unit.matches:id}",
  "reportValue": "added an updated"
}'''
    }

    @Override
    SummaryMetadataReport invalidUpdate(SummaryMetadataReport instance) {
        instance.reportValue = null
        instance
    }

    @Override
    SummaryMetadataReport validUpdate(SummaryMetadataReport instance) {
        instance.reportValue = 'added an updated'
        instance
    }

    @Override
    SummaryMetadataReport getInvalidUnsavedInstance() {
        new SummaryMetadataReport(reportValue: 'invalid',)
    }

    @Override
    SummaryMetadataReport getValidUnsavedInstance() {
        new SummaryMetadataReport(reportValue: 'valid', reportDate: dateTime.plusDays(3))
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.catalogueItemDomainType = DataModel.simpleName
        params.catalogueItemId = dataModel.id
        params.summaryMetadataId = summaryMetadata.id
    }
}