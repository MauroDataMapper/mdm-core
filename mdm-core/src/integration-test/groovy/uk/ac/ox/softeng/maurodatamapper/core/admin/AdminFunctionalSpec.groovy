/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument

import static io.micronaut.http.HttpStatus.OK

/**
 * @see AdminController* Controller: admin
 * |   POST  | /api/admin/editProperties                | Action: editApiProperties    |
 * |   POST  | /api/admin/rebuildHibernateSearchIndexes | Action: rebuildHibernateSearchIndexes |
 * |   GET   | /api/admin/properties                    | Action: apiProperties        |
 */
@Integration
@Slf4j
class AdminFunctionalSpec extends BaseFunctionalSpec {

    String getResourcePath() {
        'admin'
    }

    void 'test post to rebuildHibernateSearchIndexes'() {
        when:
        POST('rebuildHibernateSearchIndexes', [:], Argument.of(String))

        then:
        verifyJsonResponse(OK, '''{
  "user": "unlogged_user@mdm-core.com",
  "indexed": true,
  "timeTakenMilliseconds": "${json-unit.ignore}",
  "timeTaken": "${json-unit.ignore}"
}''')
    }
}
