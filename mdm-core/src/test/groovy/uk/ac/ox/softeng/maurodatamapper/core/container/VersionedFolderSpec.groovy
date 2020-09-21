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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority

import grails.testing.gorm.DomainUnitTest

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class VersionedFolderSpec extends ContainerSpec<VersionedFolder> implements DomainUnitTest<VersionedFolder> {

    @Override
    VersionedFolder newContainerClass(Map<String, Object> args) {
        new VersionedFolder(args)
    }

    @Override
    Class<VersionedFolder> getContainerClass() {
        VersionedFolder
    }

    @Override
    def setup() {
        mockDomains(Authority)
        checkAndSave(new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST))
    }

    @Override
    void setValidDomainOtherValues() {
        super.setValidDomainOtherValues()
        domain.authority = testAuthority
    }

    @Override
    Map<String, Object> getChildFolderArgs() {
        [createdBy: editor.emailAddress, label: 'child']
    }

    @Override
    Map<String, Object> getOtherFolderArgs() {
        [createdBy: editor.emailAddress, label: 'other', authority: testAuthority]
    }

    @Override
    void verifyDomainOtherConstraints(VersionedFolder subDomain) {
        super.verifyDomainOtherConstraints(subDomain)
        assert subDomain.authority == testAuthority
    }

    Authority getTestAuthority() {
        Authority.findByLabel('Test Authority')
    }

    //    void "MC01 : test "
}
