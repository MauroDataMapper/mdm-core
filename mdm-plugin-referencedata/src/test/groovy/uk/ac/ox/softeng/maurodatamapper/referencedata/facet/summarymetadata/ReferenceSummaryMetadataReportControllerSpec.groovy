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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime
import java.time.temporal.ChronoField

@Slf4j
class ReferenceSummaryMetadataReportControllerSpec extends ResourceControllerSpec<ReferenceSummaryMetadataReport> implements
    DomainUnitTest<ReferenceSummaryMetadataReport>,
    ControllerUnitTest<ReferenceSummaryMetadataReportController> {

    ReferenceDataModel referenceDataModel
    OffsetDateTime dateTime
    ReferenceSummaryMetadata referenceSummaryMetadata

    def setup() {
       mockDomains(Folder, ReferenceDataModel, ReferenceSummaryMetadata, ReferenceSummaryMetadataReport, Authority)
        log.debug('Setting up reference summary metadata controller unit')
        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.UNIT_TEST))
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.UNIT_TEST))
        referenceDataModel = new ReferenceDataModel(label: 'dm1', createdBy: StandardEmailAddress.UNIT_TEST, folder: Folder.findByLabel('catalogue'),
                                  authority: Authority.findByLabel('Test Authority'))
        checkAndSave referenceDataModel
        dateTime = OffsetDateTime.now().with(ChronoField.MILLI_OF_SECOND, 414)

        referenceSummaryMetadata = new ReferenceSummaryMetadata(createdBy: StandardEmailAddress.UNIT_TEST, label: 'summary metadata 3',
                                                                summaryMetadataType: ReferenceSummaryMetadataType.STRING)
        domain.createdBy = StandardEmailAddress.UNIT_TEST
        domain.reportValue = 'a report value'
        domain.reportDate = dateTime
        referenceSummaryMetadata
            .addToSummaryMetadataReports(domain)
            .addToSummaryMetadataReports(createdBy: StandardEmailAddress.UNIT_TEST,
                                         reportValue: 'another report',
                                         reportDate: dateTime.plusDays(1))
            .addToSummaryMetadataReports(createdBy: StandardEmailAddress.UNIT_TEST,
                                         reportValue: 'another report 2',
                                         reportDate: dateTime.plusDays(2))

        referenceDataModel.addToReferenceSummaryMetadata(referenceSummaryMetadata)

        checkAndSave(referenceDataModel)

        controller.referenceSummaryMetadataReportService = Stub(ReferenceSummaryMetadataReportService) {
            findByReferenceSummaryMetadataIdAndId(referenceSummaryMetadata.id, _) >> {UUID iid, Serializable mid ->
                if (iid != referenceSummaryMetadata.id) return null
                mid == domain.id ? domain : null
            }
            findAllByReferenceSummaryMetadataId(referenceSummaryMetadata.id, _) >> {
              log.debug("${referenceSummaryMetadata.summaryMetadataReports.toList()}")
              referenceSummaryMetadata.summaryMetadataReports.toList()
            }
            findCatalogueItemByDomainTypeAndId(ReferenceDataModel.simpleName, _) >> {String domain, UUID bid -> referenceDataModel.id == bid ? referenceDataModel : null}
        }
        controller.referenceSummaryMetadataService = Stub(ReferenceSummaryMetadataService) {
            get(referenceSummaryMetadata.id) >> referenceSummaryMetadata
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
      "message": "Property [reportDate] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata''' +
        '''.ReferenceSummaryMetadataReport] cannot be null"
    },
    {
      "message": "Property [reportValue] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata''' +
        '''.ReferenceSummaryMetadataReport] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 1,
  "errors": [
{"message": "Property [reportDate] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.''' +
        '''ReferenceSummaryMetadataReport] cannot be null"}
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
    {"message": "Property [reportValue] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.''' +
        '''ReferenceSummaryMetadataReport] cannot be null"}
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
    ReferenceSummaryMetadataReport invalidUpdate(ReferenceSummaryMetadataReport instance) {
        instance.reportValue = null
        instance
    }

    @Override
    ReferenceSummaryMetadataReport validUpdate(ReferenceSummaryMetadataReport instance) {
        instance.reportValue = 'added an updated'
        instance
    }

    @Override
    ReferenceSummaryMetadataReport getInvalidUnsavedInstance() {
        new ReferenceSummaryMetadataReport(reportValue: 'invalid',)
    }

    @Override
    ReferenceSummaryMetadataReport getValidUnsavedInstance() {
        new ReferenceSummaryMetadataReport(reportValue: 'valid', reportDate: dateTime.plusDays(3))
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.catalogueItemDomainType = ReferenceDataModel.simpleName
        params.catalogueItemId = referenceDataModel.id
        params.referenceSummaryMetadataId = referenceSummaryMetadata.id
    }
}