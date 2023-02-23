/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest
import grails.web.mime.MimeType

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class ReferenceFileSpec extends MdmDomainSpec<ReferenceFile> implements DomainUnitTest<ReferenceFile> {

    BasicModel db
    Folder misc

    def setup() {
        mockDomains(Folder, Authority)
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost', createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        checkAndSave(misc)
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc, authority: testAuthority)
        mockDomains(Folder, BasicModel)
        checkAndSave(misc)
        checkAndSave(db)
    }

    @Override
    void setValidDomainOtherValues() {
        domain.fileName = 'test'
        domain.fileType = MimeType.XML.toString()
        domain.fileContents = 'some content'.bytes
        domain.multiFacetAwareItem = db
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceFile domain) {
        domain.fileName == 'test'
        domain.fileType == MimeType.XML.toString()
        new String(domain.fileContents) == 'some content'
        domain.fileSize == 12
        domain.multiFacetAwareItemId == db.id
        domain.multiFacetAwareItemDomainType = BasicModel.simpleName
    }
}