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
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest

import java.time.OffsetDateTime

class ReferenceSummaryMetadataReportSpec extends MdmDomainSpec<ReferenceSummaryMetadataReport> implements DomainUnitTest<ReferenceSummaryMetadataReport> {

    ReferenceDataModel db
    Folder misc
    ReferenceSummaryMetadata referenceSummaryMetadata
    OffsetDateTime dateTime

    def setup() {
        mockDomains(Folder, ReferenceDataModel, ReferenceSummaryMetadata, Authority)
        misc = new Folder(createdBy: StandardEmailAddress.UNIT_TEST, label: 'misc')
        checkAndSave(misc)

        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.UNIT_TEST))
        db = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test', folder: misc,
                                    authority: Authority.findByLabel('Test Authority'))

        checkAndSave(db)
        referenceSummaryMetadata = new ReferenceSummaryMetadata(summaryMetadataType: ReferenceSummaryMetadataType.NUMBER, label: 'test',
                                                                createdBy: StandardEmailAddress.UNIT_TEST, multiFacetAwareItem: db)
        checkAndSave(referenceSummaryMetadata)
        dateTime = OffsetDateTime.now()
    }

    @Override
    void setValidDomainOtherValues() {
        domain.reportDate = dateTime
        domain.reportValue = 'some text'
        referenceSummaryMetadata.addToSummaryMetadataReports(domain)
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceSummaryMetadataReport domain) {
        assert domain.reportDate == dateTime
        assert domain.reportValue == 'some text'
    }
}
