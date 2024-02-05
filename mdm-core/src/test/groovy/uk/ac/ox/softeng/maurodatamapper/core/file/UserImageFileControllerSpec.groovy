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
package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFileController
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFileService
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import java.nio.file.Path
import java.nio.file.Paths

import static io.micronaut.http.HttpStatus.OK

/**
 * @since 23/01/2018
 */
@Slf4j
class UserImageFileControllerSpec extends ResourceControllerSpec<UserImageFile> implements ControllerUnitTest<UserImageFileController>,
    DomainUnitTest<UserImageFile> {

    static Path defaultImagePath = Paths.get('src/test/resources/NoProfileSmall.jpeg')
    static UserImageFile defaultProfilePic
    static UserImageFileService userImageFileService
    static byte[] resizedImage = [-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 10, 0, 0, 0, 10, 8, 2, 0, 0, 0, 2, 80,
                                  88, -22, 0, 0, 0, -70, 73, 68, 65, 84, 120, 94, 85, -114, 75, 10, -124, 64, 12, 68, -93, -82, 84, -16, -117, 8,
                                  -83, 32, 110, 90, -68, -121, -32, -103, -36, 121, 35, 65, -36, -10, -91, -60, -115, -8, 105, -85, 39, -61, -32,
                                  -44, 38, -99, 122, -99, 74, 104, -33, -9, -29, 56, -18, -5, -42, 90, -81, -21, -70, 109, -101, -2, -120, 29, 58,
                                  -49, -109, -5, -86, -86, -62, 48, -52, -78, 76, 74, -55, 24, 34, -68, 48, -35, 117, -99, -17, -5, 68, 100, -37,
                                  -74, -21, -70, 125, -33, -125, 93, -41, 101, 48, -28, 121, 30, -67, -124, 24, -10, 13, -58, -57, 52, 77, -33,
                                  -40, -78, -84, -17, 110, 46, 65, 16, -68, 113, 20, 69, 127, -31, -61, 48, -16, 110, -57, 113, -30, 56, 30,
                                  -57, 17, -90, -63, -53, -78, -16, 4, -25, -29, -76, 60, -49, -39, 81, 74, -103, -100, -78, 44, -71, -57,
                                  -127, -65, 45, 117, 93, 39, 73, 98, -62, -89, 105, 106, -37, 86, 8, -127, 30, -55, 69, 81, 52, 77, 51, -49,
                                  51, -48, 3, 95, -89, -124, -123, 60, 122, 51, -61, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126]

    def setupSpec() {
        log.debug('Setting up user image file controller unit spec')
        userImageFileService = new UserImageFileService()
        defaultProfilePic = userImageFileService.createNewFile(defaultImagePath, UnloggedUser.instance)
    }

    def setup() {
        log.debug('Setting up image file controller unit')
        mockDomains(Edit)
        controller.userImageFileService = userImageFileService
        domain.fileContents = defaultProfilePic.fileContents
        domain.fileType = defaultProfilePic.fileType
        domain.userId = randomId2
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

    void 'test show renders correctly with a resize'() {
        when:
        params.size = 10
        params.id = domain.id
        controller.show()

        then:
        verifyResponse(OK)

        and: 'content is rendered as a file not bytes'
        response.contentType == 'image/png;charset=utf-8'
        response.contentAsByteArray == resizedImage
    }


    @Override
    String getExpectedIndexJson() {
        """{
  "count": 1,
  "items": [
    {
      "lastUpdated": "\${json-unit.matches:offsetDateTime}",
      "fileName": "${randomId2}-profile",
      "domainType": "UserImageFile",
      "fileSize": 1395,
      "id": "\${json-unit.matches:id}",
      "userId": "${randomId2}",
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
      "message": "Property [fileSize] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile] cannot be null"
    },
    {
      "message": "Property [fileType] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile] cannot be null"
    },
    {
      "message": "Property [fileContents] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile] cannot be null"
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
      "message": "Property [fileSize] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile] cannot be null"
    },
    {
      "message": "Property [fileContents] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile] cannot be null"
    }
  ]
}
'''
    }

    @Override
    String getExpectedValidSavedJson() {
        """{
      "lastUpdated": "\${json-unit.matches:offsetDateTime}",
      "fileName": "${randomId2}-profile",
      "domainType": "UserImageFile",
      "fileSize": 1395,
      "id": "\${json-unit.matches:id}",
      "userId": "${randomId2}",
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
      "message": "Property [fileType] of class [class uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedValidUpdatedJson() {
        """{
      "lastUpdated": "\${json-unit.matches:offsetDateTime}",
      "fileName": "${randomId2}-profile",
      "domainType": "UserImageFile",
      "fileSize": 243,
      "id": "\${json-unit.matches:id}",
      "userId": "${randomId2}",
      "fileType": "image/png"
    }"""
    }

    @Override
    UserImageFile invalidUpdate(UserImageFile instance) {
        instance.fileType = ''
        instance
    }

    @Override
    UserImageFile validUpdate(UserImageFile instance) {
        UserImageFile image = userImageFileService.resizeImage(instance, 10)
        image.id = instance.id
        image
    }

    @Override
    UserImageFile getInvalidUnsavedInstance() {
        new UserImageFile(fileType: 'png')
    }

    @Override
    UserImageFile getValidUnsavedInstance() {
        UserImageFile userImageFile = userImageFileService.createNewFile(defaultImagePath, UnloggedUser.instance)
        userImageFile.userId = randomId2
        userImageFile
    }

    @Override
    String getTemplate() {
        '''
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile

model {
    UserImageFile userImageFile
}

json {
    image userImageFile.fileContents?.encodeBase64()?.toString()
    type userImageFile.fileType
    userId userImageFile.userId
}
'''
    }
}
