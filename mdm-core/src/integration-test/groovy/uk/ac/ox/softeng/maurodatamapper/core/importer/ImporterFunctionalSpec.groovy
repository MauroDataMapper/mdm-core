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
package uk.ac.ox.softeng.maurodatamapper.core.importer

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument

import static io.micronaut.http.HttpStatus.NOT_FOUND

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterController* Controller: importer
 *  | GET | /api/importer/parameters/${ns}?/${name}?/${version}? | Action: parameters |
 */
@Integration
@Slf4j
class ImporterFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'importer'
    }

    void 'test importer parameters'() {

        when:
        GET('parameters/ox.softeng.maurodatamapper.core.spi.json/JsonImporterService/1.1', Argument.of(String))

        then:
        verifyJsonResponse(NOT_FOUND, '''{
  "path": "/api/importer/parameters/ox.softeng.maurodatamapper.core.spi.json/JsonImporterService/1.1",
  "resource": "ImporterProviderService",
  "id": "ox.softeng.maurodatamapper.core.spi.json:JsonImporterService:1.1"
}''')
    }
}
