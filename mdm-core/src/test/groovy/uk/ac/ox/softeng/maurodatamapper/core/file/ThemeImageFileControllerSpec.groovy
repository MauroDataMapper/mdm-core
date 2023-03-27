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
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import java.nio.file.Path
import java.nio.file.Paths

import static io.micronaut.http.HttpStatus.OK

/**
 * @since 15/03/2023
 */
@Slf4j
class ThemeImageFileControllerSpec extends ResourceControllerSpec<ThemeImageFile> implements ControllerUnitTest<ThemeImageFileController>,
    DomainUnitTest<ThemeImageFile> {

    static Path defaultImagePath = Paths.get('src/test/resources/NoProfileSmall.jpeg')
    static ThemeImageFile defaultProfilePic
    static ThemeImageFileService themeImageFileService
    static ApiPropertyService apiPropertyService

    def setupSpec() {
        log.debug('Setting up user image file controller unit spec')
        themeImageFileService = new ThemeImageFileService()
        defaultProfilePic = themeImageFileService.createNewFile(defaultImagePath, UnloggedUser.instance)
        apiPropertyService = new ApiPropertyService()
    }

    def setup() {
        log.debug('Setting up image file controller unit')
        mockDomains(Edit, ApiProperty)
        controller.themeImageFileService = themeImageFileService
        controller.apiPropertyService = apiPropertyService
        domain.fileContents = defaultProfilePic.fileContents
        domain.fileType = defaultProfilePic.fileType
        domain.createdBy = UnloggedUser.instance.emailAddress
        checkAndSave(domain)
    }

    void 'test show renders correctly'() {
        when:
        params.id = domain.id
        controller.show()

        then:
        verifyResponse(OK)

        and: 'content is rendered as a file not bytes'
        response.contentType == 'image/jpeg;charset=utf-8'
        response.contentAsByteArray == defaultProfilePic.fileContents
    }


    @Override
    String getExpectedIndexJson() {
        """{
  "count": 1,
  "items": [
    {
      "lastUpdated": "\${json-unit.matches:offsetDateTime}",
      "fileName": "\${json-unit.matches:themeImageFileName}",
      "domainType": "ThemeImageFile",
      "fileSize": 1395,
      "id": "\${json-unit.matches:id}",
      "fileType": "image/jpeg"
    }
  ]
}"""
    }

    @Override
    String getExpectedNullSavedJson() {
        '''{
  "total": 3,
  "errors": [
    {
      "message": "Property [fileSize] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFile] cannot be null"
    },
    {
      "message": "Property [fileType] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFile] cannot be null"
    },
    {
      "message": "Property [fileContents] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFile] cannot be null"
    }
  ]
}
'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 2,
  "errors": [
    {
      "message": "Property [fileSize] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFile] cannot be null"
    },
    {
      "message": "Property [fileContents] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFile] cannot be null"
    }
  ]
}
'''
    }

    @Override
    String getExpectedValidSavedJson() {
        """{
      "lastUpdated": "\${json-unit.matches:offsetDateTime}",
      "fileName": "\${json-unit.matches:themeImageFileName}",
      "domainType": "ThemeImageFile",
      "fileSize": 1395,
      "id": "\${json-unit.matches:id}",
      "fileType": "image/jpeg"
    }"""
    }

    @Override
    String getExpectedShowJson() {
        '{}'
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '''{
  "total": 1,
  "errors": [
    {
      "message": "Property [fileType] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFile] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedValidUpdatedJson() {
        """{
      "lastUpdated": "\${json-unit.matches:offsetDateTime}",
      "fileName": "\${json-unit.matches:themeImageFileName}",
      "domainType": "ThemeImageFile",
      "fileSize": 1395,
      "id": "\${json-unit.matches:id}",
      "fileType": "image/jpeg"
    }"""
    }

    @Override
    ThemeImageFile invalidUpdate(ThemeImageFile instance) {
        instance.fileType = ''
        instance
    }

    @Override
    ThemeImageFile validUpdate(ThemeImageFile instance) {
        instance
    }

    @Override
    ThemeImageFile getInvalidUnsavedInstance() {
        new ThemeImageFile(fileType: 'png')
    }

    @Override
    ThemeImageFile getValidUnsavedInstance() {
        ThemeImageFile themeImageFile = themeImageFileService.createNewFile(defaultImagePath, UnloggedUser.instance)
        themeImageFile
    }

    @Override
    String getTemplate() {
        '''
import uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFile

model {
    ThemeImageFile themeImageFile
}

json {
    image themeImageFile.fileContents?.encodeBase64()?.toString()
    type themeImageFile.fileType
}
'''
    }
}
