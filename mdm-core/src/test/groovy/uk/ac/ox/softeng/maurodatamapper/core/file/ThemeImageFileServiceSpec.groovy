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
package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest
import grails.web.mime.MimeType

import java.nio.file.Files
import java.nio.file.Paths

class ThemeImageFileServiceSpec extends BaseUnitSpec implements ServiceUnitTest<ThemeImageFileService> {

    UUID id
    ThemeImageFile themeImageFile
    ApiProperty apiProperty
    ApiProperty apiProperty_defaultImage
    UserImageFileService userImageFileService = new UserImageFileService()

    def setup() {
        mockDomains(UserImageFile, ThemeImageFile, ApiProperty)
        checkAndSave service.createNewFile(Paths.get('src/test/resources/userimagefile_string_content.txt'), admin)
        themeImageFile = service.createNewFile('test2', 'jhsdkjfhsdgfsdnmbhfjhsdjghsdjgjfhsd'.bytes,
                                                            MimeType.MULTIPART_FORM.toString(), editor)
        checkAndSave(themeImageFile)
        checkAndSave service.createNewFile(Paths.get('src/test/resources/userimagefile_string_content.txt'), reader1)
        checkAndSave service.createNewFile('testfile', 'this is a test file'.bytes, MimeType.ALL.toString(), reader2)
        checkAndSave userImageFileService.createNewFile('testfile_userImage', 'this is a test user image file'.bytes, MimeType.ALL.toString(), reader2)
        id = themeImageFile.id

        apiProperty = new ApiProperty(
            key: "explorer.theme.images.header.logo",
            value: "${themeImageFile.id}",
            lastUpdatedBy: 'user@test.com',
            createdBy: 'user@test.com')

        apiProperty_defaultImage = new ApiProperty(
            key: "explorer.theme.images.header.logotwo",
            value: "USE DEFAULT IMAGE",
            lastUpdatedBy: 'user@test.com',
            createdBy: 'user@test.com')

        checkAndSave(apiProperty)
        checkAndSave(apiProperty_defaultImage)

    }

    void 'test get'() {
        expect:
        service.get(id) != null
    }

    void 'test list'() {

        when:
        List<ThemeImageFile> themeImageFileList = service.list(max: 2, offset: 1)

        then:
        themeImageFileList.size() == 2

        and:
        themeImageFileList[0].fileName == 'test2'
        themeImageFileList[0].fileType == MimeType.MULTIPART_FORM.toString()
        themeImageFileList[0].createdBy == editor.emailAddress
        themeImageFileList[0].fileContents == 'jhsdkjfhsdgfsdnmbhfjhsdjghsdjgjfhsd'.bytes

        and:
        themeImageFileList[1].fileName == 'userimagefile_string_content.txt'
        themeImageFileList[1].fileType in ['text/plain', 'Unknown']
        themeImageFileList[1].createdBy == reader1.emailAddress
        themeImageFileList[1].fileSize
        themeImageFileList[1].fileSize == Files.size(Paths.get('src/test/resources/userimagefile_string_content.txt'))
    }

    void 'test count'() {
        expect:
        service.count() == 4
    }

    void 'test delete'() {
        expect:
        service.count() == 4

        when:
        service.delete(id)

        then:
        service.count() == 3
    }

    void 'test save'() {
        when:
        ThemeImageFile themeImageFile = service.createNewFile(Paths.get('src/test/resources/userimagefile_string_content.txt'), reader1)
        checkAndSave(themeImageFile)

        then:
        themeImageFile.id != null
    }

    void 'test findByApiPropertyId'() {
        when:
        ThemeImageFile themeImageFile = service.findByApiPropertyId(apiProperty.id)
        ThemeImageFile themeImageFile_Missing = service.findByApiPropertyId(null)

        then:
        themeImageFile.id.toString() == apiProperty.value
        themeImageFile_Missing == null
    }

    void 'test findByApiProperty'() {
        when:
        ThemeImageFile themeImageFile = service.findByApiProperty(apiProperty)
        ThemeImageFile themeImageFile_Missing = service.findByApiProperty(null)

        then:
        themeImageFile.id.toString() == apiProperty.value
        themeImageFile_Missing == null
    }


}
