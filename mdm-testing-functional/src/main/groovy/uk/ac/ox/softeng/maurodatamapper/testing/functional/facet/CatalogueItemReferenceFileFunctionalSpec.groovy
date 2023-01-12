/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet

import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: referenceFile
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: delete
 *  |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: update
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileController
 */
@Slf4j
abstract class CatalogueItemReferenceFileFunctionalSpec extends UserAccessFunctionalSpec {

    abstract String getModelId()

    abstract String getCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/referenceFiles"
    }

    @Override
    String getEditsFullPath(String id) {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}"
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .withoutAvailableActions()
            .whereAuthors {
                cannotEditDescription()
                cannotUpdate()
            }
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    @Override
    void verify02Response(HttpResponse<Map> response, String id, List<String> actions) {
        verifyResponse OK, response
        assert response.contentLength == 6
        assert response.header('Content-Disposition') == 'attachment;filename="functional test file.txt"'
        assert response.header('Content-Type') == 'application/octet-stream;charset=utf-8'
    }

    @Override
    void verify03ValidResponseBody(HttpResponse<Map> response) {
        assert responseBody().id
        assert responseBody().fileName == 'functional test file.txt'
        assert responseBody().fileType == 'text/plain'
        assert responseBody().fileSize == 6
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[ReferenceFile:functional test file.txt] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[ReferenceFile:updated filename.txt] changed properties \[path, fileName]/
    }

    @Override
    Map getValidJson() {
        [
            fileName    : 'functional test file.txt',
            fileContents: [104, 101, 108, 108, 111, 10],
            fileType    : 'text/plain',
            fileSize    : 6
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            fileName    : '',
            fileContents: [104, 101, 108, 108, 111, 10],
            fileType    : 'text/plain',
            fileSize    : 6
        ]
    }

    @Override
    Map getValidNonDescriptionUpdateJson() {
        [
            fileName: 'updated filename.txt'
        ]
    }

    @Override
    Map getValidDescriptionOnlyUpdateJson() {
        getValidNonDescriptionUpdateJson()
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": []
}'''
    }
}