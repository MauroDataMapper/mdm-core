/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.test.functional.facet


import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

/**
 * <pre>
 * Controller: referenceFile
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles       | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles       | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id} | Action: delete
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id} | Action: update
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id} | Action: show
 * </pre>
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileController
 */
@Slf4j
abstract class CatalogueItemReferenceFileFunctionalSpec extends CatalogueItemFacetFunctionalSpec<ReferenceFile> {

    @Override
    String getFacetResourcePath() {
        'referenceFiles'
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
    Map getValidUpdateJson() {
        [
            fileName: 'updated filename.txt'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "ReferenceFile",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "fileSize": 6,
    "fileType": "text/plain",
    "fileName": "test3.txt"
    }'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        responseBody().fileName == 'updated filename.txt'
    }

    @Override
    void verifyR5ShowResponse() {
        verifyResponse(HttpStatus.OK, jsonCapableResponse)
        assert jsonCapableResponse.body() == 'hello\n'
        assert jsonCapableResponse.header('Content-Disposition') == 'attachment;filename="functional test file.txt"'
        assert jsonCapableResponse.header('Content-Type') == 'application/octet-stream;charset=utf-8'
    }
}