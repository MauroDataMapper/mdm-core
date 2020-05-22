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
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFileService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest
import grails.web.mime.MimeType

import java.nio.file.Files
import java.nio.file.Paths

class UserImageFileServiceSpec extends BaseUnitSpec implements ServiceUnitTest<UserImageFileService> {

    UUID id

    def setup() {
        mockDomain(UserImageFile)
        checkAndSave service.createNewFile(Paths.get('grails-app/conf/logback.groovy'), admin)
        UserImageFile userImageFile = service.createNewFile('test2', 'jhsdkjfhsdgfsdnmbhfjhsdjghsdjgjfhsd'.bytes,
                                                            MimeType.MULTIPART_FORM.toString(), editor)
        checkAndSave(userImageFile)
        checkAndSave service.createNewFile(Paths.get('grails-app/conf/logback.groovy'), reader1)
        checkAndSave service.createNewFile('testfile', 'this is a test file'.bytes, MimeType.ALL.toString(), reader2)
        id = userImageFile.id
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {

        when:
        List<UserImageFile> userImageFileList = service.list(max: 2, offset: 1)

        then:
        userImageFileList.size() == 2

        and:
        userImageFileList[0].fileName == 'test2'
        userImageFileList[0].fileType == MimeType.MULTIPART_FORM.toString()
        userImageFileList[0].createdBy == editor.emailAddress
        userImageFileList[0].fileContents == 'jhsdkjfhsdgfsdnmbhfjhsdjghsdjgjfhsd'.bytes
        userImageFileList[0].userId == editorId

        and:
        userImageFileList[1].fileName == 'logback.groovy'
        userImageFileList[1].fileType in ['text/plain', 'Unknown']
        userImageFileList[1].createdBy == reader1.emailAddress
        userImageFileList[1].fileSize
        userImageFileList[1].fileSize == Files.size(Paths.get('grails-app/conf/logback.groovy'))
        userImageFileList[1].userId == reader1Id
    }

    void "test count"() {
        expect:
        service.count() == 4
    }

    void "test delete"() {
        expect:
        service.count() == 4

        when:
        service.delete(id)

        then:
        service.count() == 3
    }

    void "test save"() {
        when:
        UserImageFile userImageFile = service.createNewFile(Paths.get('grails-app/conf/logback.groovy'), reader1)
        checkAndSave(userImageFile)

        then:
        userImageFile.id != null
    }

    void 'test image resize'() {
        when:
        UserImageFile userImageFile = service.createNewFile(Paths.get('grails-app/assets/images/NoProfile.jpg'), admin)
        checkAndSave(userImageFile)

        then:
        userImageFile.id
        userImageFile.fileSize == 10405

        when:
        UserImageFile resize = service.resizeImage(userImageFile, 20)
        checkAndSave(resize)

        then:
        resize.id != userImageFile.id
        resize.fileSize < userImageFile.fileSize
        resize.fileSize == 221

    }
}
