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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.admin

import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Unroll

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: admin
 *  |  GET   | /api/admin/status                | Action: status
 *  |  POST  | /api/admin/rebuildHibernateSearchIndexes  | Action: rebuildHibernateSearchIndexes
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.admin.AdminController
 */
@Integration
@Slf4j
class AdminFunctionalSpec extends FunctionalSpec {

    String getResourcePath() {
        'admin'
    }

    @Unroll
    void '#method:#endpoint endpoint are admin access only'() {
        when: 'Unlogged in call to check'
        if (args != null) this."$method"(endpoint, args)
        else this."$method"(endpoint)

        then: 'The response is Unauth'
        verifyForbidden response

        when: 'logged in as normal user'
        loginAuthenticated()
        if (args != null) this."$method"(endpoint, args)
        else this."$method"(endpoint)

        then: 'The response is Unauth'
        verifyForbidden response

        where:
        method | endpoint               | args
        'GET'  | 'status'               | null
        'POST' | 'rebuildHibernateSearchIndexes' | [:]
    }

    @Unroll
    void '#method:#endpoint endpoint when logged in as admin user'() {
        when: 'logged in as admin'
        loginAdmin()
        if (args != null) this."$method"(endpoint, args, STRING_ARG)
        else this."$method"(endpoint, STRING_ARG)

        then: 'The response is Unauth'
        verifyJsonResponse responseCode, expectedJson

        where:
        method | endpoint               | args                                       || responseCode | expectedJson
        'GET'  | 'status'               | null                                       || OK           | '''
{
  "Mauro Data Mapper Version": "${json-unit.matches:version}",
  "Grails Version": "5.3.0",
  "Java Version": "${json-unit.matches:version}",
  "Java Vendor": "${json-unit.any-string}",
  "OS Name": "${json-unit.any-string}",
  "OS Version": "${json-unit.matches:version}",
  "OS Architecture": "${json-unit.any-string}",
  "Driver Manager Drivers Available": [
    {
      "class": "org.h2.Driver",
      "version": "1.4"
    },
    {
      "class": "org.postgresql.Driver",
      "version": "42.3"
    }
  ]
}
'''

        'POST' | 'rebuildHibernateSearchIndexes' | [:]                                        || OK           | '''{
  "user": "admin@maurodatamapper.com",
  "indexed": true,
  "timeTakenMilliseconds": "${json-unit.ignore}",
  "timeTaken": "${json-unit.ignore}"
}'''
    }
}
