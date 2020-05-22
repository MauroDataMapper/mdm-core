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
package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import grails.web.mime.MimeType

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class UserImageFileSpec extends CreatorAwareSpec<UserImageFile> implements DomainUnitTest<UserImageFile> {

    void 'test creating actual user image'() {
        given:
        Path p = Paths.get('src/test/resources/userimagefile_string_content.txt')
        assert Files.exists(p)
        String image = Files.readString(p)

        when:
        domain.setImage(image)
        domain.setType('png')
        domain.setCreatedBy(editor.emailAddress)
        domain.setUserId(editor.id)
        checkAndSave(domain)

        then:
        noExceptionThrown()
    }

    @Override
    void setValidDomainOtherValues() {
        domain.fileName = 'test'
        domain.fileType = MimeType.XML.toString()
        domain.fileContents = 'some content'.bytes
        domain.userId = UUID.randomUUID()
    }

    @Override
    void verifyDomainOtherConstraints(UserImageFile domain) {
        domain.fileName == 'test'
        domain.fileType == MimeType.XML.toString()
        new String(domain.fileContents) == 'some content'
        domain.fileSize == 12
    }
}
