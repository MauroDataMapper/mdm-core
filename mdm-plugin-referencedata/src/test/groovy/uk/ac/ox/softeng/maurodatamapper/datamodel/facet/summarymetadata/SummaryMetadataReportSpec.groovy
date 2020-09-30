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
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest

import java.time.OffsetDateTime

class SummaryMetadataReportSpec extends CreatorAwareSpec<SummaryMetadataReport> implements DomainUnitTest<SummaryMetadataReport> {

    DataModel db
    Folder misc
    SummaryMetadata summaryMetadata
    OffsetDateTime dateTime

    def setup() {
        mockDomains(Folder, DataModel, SummaryMetadata, Authority)
        misc = new Folder(createdBy: StandardEmailAddress.UNIT_TEST, label: 'misc')
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.UNIT_TEST))
        db = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test', folder: misc, authority: Authority.findByLabel('Test Authority'))
        checkAndSave(misc)
        checkAndSave(db)
        summaryMetadata = new SummaryMetadata(summaryMetadataType: SummaryMetadataType.NUMBER, label: 'test',
                                              createdBy: StandardEmailAddress.UNIT_TEST, catalogueItem: db)
        checkAndSave(summaryMetadata)
        dateTime = OffsetDateTime.now()
    }

    @Override
    void setValidDomainOtherValues() {
        domain.reportDate = dateTime
        domain.reportValue = 'some text'
        summaryMetadata.addToSummaryMetadataReports(domain)
    }

    @Override
    void verifyDomainOtherConstraints(SummaryMetadataReport domain) {
        assert domain.reportDate == dateTime
        assert domain.reportValue == 'some text'
    }
}
