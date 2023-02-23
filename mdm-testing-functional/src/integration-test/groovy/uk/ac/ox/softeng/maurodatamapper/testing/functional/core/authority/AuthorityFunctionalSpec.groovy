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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.authority

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
class AuthorityFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        'authorities'
    }

    @Transactional
    Authority getDefaultAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereEditorsCannotChangePermissions()
            .whereEditors {
                cannotCreate()
                cannotDelete()
                cannotEditDescription()
                cannotUpdate()
                canAction('show')
            }
            .whereAuthors {
                cannotEditDescription()
                cannotUpdate()
            }
            .whereAuthenticatedUsers {
                canSee()
                canIndex()
                canAction('show')
            }
            .whereAnonymousUsers {
                canIndex()
            }
    }

    @RunOnce
    @Transactional
    def setup() {
        log.info('Adding application admin group who can edit authorities, contains the user containerAdmin')
        CatalogueUser containerAdmin = getUserByEmailAddress(userEmailAddresses.containerAdmin)
        // To allow testing of reader group rights which reader2 is not in
        UserGroup group = new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'applicationAdmin',
            applicationGroupRole: GroupRole.findByName(GroupRole.APPLICATION_ADMIN_ROLE_NAME)
        ).addToGroupMembers(containerAdmin)
        checkAndSave(messageSource, group)
    }

    @Transactional
    def cleanupSpec() {
        UserGroup.findByName('applicationAdmin').delete(flush: true)
    }

    @Override
    Map getValidJson() {
        [
            url  : 'https://functional-spec-authority.com',
            label: 'Functional Spec Authority'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            url  : getDefaultAuthority().url,
            label: getDefaultAuthority().label
        ]
    }

    void verify01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        assert response.body().count == 1
        assert response.body().items.size() == 1
    }

    @Override
    void verifyNA04Response(HttpResponse<Map> response, String id) {
        verifyForbidden(response)
    }

    @Override
    List<String> getPermanentGroupNames() {
        super.getPermanentGroupNames() + ['applicationAdmin']
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[.+?@.+?] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[.+?@.+?] changed properties \[path, label]/
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper",
      "defaultAuthority": true
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "url": "https://functional-spec-authority.com",
  "label": "Functional Spec Authority",
  "defaultAuthority": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": [
    "show"
  ]
}'''
    }

}

