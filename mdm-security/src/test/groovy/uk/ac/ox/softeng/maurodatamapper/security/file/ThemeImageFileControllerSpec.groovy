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
package uk.ac.ox.softeng.maurodatamapper.security.file

import uk.ac.ox.softeng.maurodatamapper.core.admin.AdminService
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFile
import uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFileController
import uk.ac.ox.softeng.maurodatamapper.core.file.ThemeImageFileService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import net.javacrumbs.jsonunit.core.Option

import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
class ThemeImageFileControllerSpec extends ResourceControllerSpec<ThemeImageFile> implements ControllerUnitTest<ThemeImageFileController>,
    DomainUnitTest<ThemeImageFile>, SecurityUsers {

    static Path defaultImagePath = Paths.get('src/test/resources/NoProfileSmall.jpeg')
    static Path newImagePath = Paths.get('src/test/resources/NewProfileImage.png')
    static ThemeImageFile defaultProfilePic
    static ThemeImageFile newProfilePic
    static ThemeImageFileService themeImageFileService = new ThemeImageFileService()
    ApiProperty apiProperty
    ThemeImageFile themeImageFile
    UUID invalidUUID

    def setupSpec() {
        log.debug('Setting up theme image file controller unit spec')
        defaultProfilePic = themeImageFileService.createNewFile(defaultImagePath, UnloggedUser.instance)
        newProfilePic = themeImageFileService.createNewFile(newImagePath, UnloggedUser.instance)
    }

    def setup() {
        log.debug('Setting up ThemeImageFileControllerSpec')
        mockDomains(CatalogueUser, Edit, ApiProperty)
        implementSecurityUsers('unitTest')

        invalidUUID = UUID.randomUUID()

        apiProperty = new ApiProperty(
            key: "explorer.theme.images.header.logo",
            value: "USE DEFAULT IMAGE",
            lastUpdatedBy: 'user@test.com',
            createdBy: 'user@test.com')

        checkAndSave(apiProperty)

        themeImageFile = themeImageFileService.createNewFile(newImagePath, UnloggedUser.instance)
        checkAndSave(themeImageFile)

        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        params.apiPropertyId = apiProperty.id

        controller.apiPropertyService = Mock(ApiPropertyService) {
            findById(_) >> { UUID id ->
                apiProperty
            }

            save(_,_) >> { ApiProperty apiPropertyInput, User updatedBy ->
                apiProperty = apiPropertyInput
                apiProperty.lastUpdatedBy = updatedBy.emailAddress
                checkAndSave(apiProperty)
            }
        }

        controller.themeImageFileService = Mock(ThemeImageFileService) {
            findByApiPropertyId(_) >> { UUID id ->
                ApiProperty foundApiProperty = ApiProperty.findById(id)
                if (foundApiProperty && foundApiProperty.value != "USE DEFAULT IMAGE") {
                    return ThemeImageFile.findById(foundApiProperty.value)
                }
                null
            }
            apiPropertyHasImage(_) >> { UUID id ->
                return apiProperty.value != "USE DEFAULT IMAGE"
            }
            list(_) >> []
            delete(_) >> { ThemeImageFile r ->
                themeImageFileService.delete(r)
            }
        }

        if (testIndex in ['R1', 'R2.3', 'R3.3', 'R4.3', 'R5.3']) {
            params.apiPropertyId = apiProperty.id
        } else if (testIndex in ['R2.2', 'R3.2', 'R4.2', 'R5.2']) {
            params.apiPropertyId = invalidUUID
        } else if (testIndex in ['R2.1', 'R3.1', 'R4.1', 'R5.1']) {
            params.apiPropertyId = null
        }

        if (testIndex in ['R3.3', 'R4.2', 'R4.3', 'R5.3']) {
            domain.fileContents = defaultProfilePic.fileContents
            domain.fileType = defaultProfilePic.fileType
            domain.createdBy = userEmailAddresses.unitTest
            checkAndSave(domain)
        }

        if (testIndex in ['R3.3', 'R4.3']) {
            apiProperty.value = themeImageFile.id
            checkAndSave(apiProperty)
        }
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.userId = editor.id
    }

    @Override
    void verifyR22SaveInvalidIdResponse() {
        verifyJsonResponse HttpStatus.NOT_FOUND,  """{
    "id":"${invalidUUID}",
    "resource":"ThemeImageFile"
}""", Option.IGNORING_VALUES
    }

    @Override
    void verifyR53DeleteActionWithAnInstanceResponse() {
        verifyResponse HttpStatus.OK
    }

    void verifyR42UpdateInvalidInstanceResponse() {
        verifyJsonResponse HttpStatus.NOT_FOUND, """{
    "id":"${invalidUUID}",
    "resource":"ThemeImageFile"
}""", Option.IGNORING_VALUES
    }

    void 'test getting theme image when no image set for unknown apiProperty'() {
        when:
        params.apiPropertyId = invalidUUID
        controller.show()

        then:
        verifyResponse HttpStatus.NOT_FOUND
    }

    void 'test getting theme image when no image set'() {
        when:
        params.apiPropertyId = apiProperty.id
        controller.show()

        then:
        verifyResponse HttpStatus.NOT_FOUND
    }


    @Override
    String getExpectedIndexJson() {
        '{"count": 0,"items": []}'
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
    String getExpectedValidSavedJson() {
        """{
      "lastUpdated": "\${json-unit.matches:offsetDateTime}",
      "fileName": "${apiProperty.id}-theme",
      "domainType": "ThemeImageFile",
      "fileSize": 5094,
      "id": "\${json-unit.matches:id}",
      "fileType": "image/png"
    }""".toString()
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
      "fileName": "NewProfileImage.png",
      "domainType": "ThemeImageFile",
      "fileSize": 5094,
      "id": "\${json-unit.matches:id}",
      "fileType": "image/png"
    }""".toString()
    }

    @Override
    ThemeImageFile invalidUpdate(ThemeImageFile instance) {
        instance.fileType = ''
        instance
    }

    @Override
    ThemeImageFile validUpdate(ThemeImageFile instance) {
        newProfilePic
    }

    @Override
    ThemeImageFile getInvalidUnsavedInstance() {
        new ThemeImageFile()
    }

    @Override
    ThemeImageFile getValidUnsavedInstance() {
        newProfilePic
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
