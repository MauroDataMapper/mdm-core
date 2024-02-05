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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class ReferenceSummaryMetadataSpec extends MdmDomainSpec<ReferenceSummaryMetadata> implements DomainUnitTest<ReferenceSummaryMetadata> {

    ReferenceDataModel db
    Folder misc

    def setup() {
        misc = new Folder(createdBy: StandardEmailAddress.UNIT_TEST, label: 'misc')
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost')
        db = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test', folder: misc, authority: testAuthority)
        mockDomains(Folder, ReferenceDataModel, ReferenceSummaryMetadata)
        checkAndSave(misc)
        checkAndSave(db)
    }

    void 'test no catalogue item'() {
        given:
        domain.summaryMetadataType = ReferenceSummaryMetadataType.NUMBER
        domain.createdBy = StandardEmailAddress.UNIT_TEST

        when:
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.getFieldError('multiFacetAwareItemId')
        domain.errors.getFieldError('multiFacetAwareItemDomainType')
    }

    @Override
    void setValidDomainOtherValues() {
        domain.summaryMetadataType = ReferenceSummaryMetadataType.NUMBER
        domain.label = 'test'
        domain.multiFacetAwareItem = db
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceSummaryMetadata domain) {
        assert domain.summaryMetadataType == ReferenceSummaryMetadataType.NUMBER
        assert domain.multiFacetAwareItem.id
        assert domain.multiFacetAwareItemDomainType == ReferenceDataModel.simpleName
        assert domain.multiFacetAwareItem.id == db.id
        assert domain.createdBy == admin.emailAddress
        assert domain.label == 'test'
    }
}