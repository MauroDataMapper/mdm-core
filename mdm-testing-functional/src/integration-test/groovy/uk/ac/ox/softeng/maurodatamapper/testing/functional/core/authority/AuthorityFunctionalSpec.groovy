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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.authority

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
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

    @OnceBefore
    @Transactional
    @Override
    def addValidIdReaderGroup() {
        log.info('Not adding valid id reader group')
    }

    @OnceBefore
    @Transactional
    def addApplicationAdminGroup() {
        log.info('Adding application admin group who can edit authorities')
        CatalogueUser editor = getUserByEmailAddress(userEmailAddresses.editor)
        // To allow testing of reader group rights which reader2 is not in
        UserGroup group = new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'applicationAdmin',
            applicationGroupRole: GroupRole.findByName(GroupRole.APPLICATION_ADMIN_ROLE_NAME)
        ).addToGroupMembers(editor)
        checkAndSave(messageSource, group)
    }

    @Transactional
    def cleanupSpec() {
        UserGroup.findByName('applicationAdmin').delete(flush: true)
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'This was created by functional testing'
        ]
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

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    Boolean getReaderCanSeeEditorCreatedItems() {
        false
    }

    void verifyR01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        assert response.body().count == 1
        assert response.body().items.size() == 1
    }

    @Override
    void verifyL01Response(HttpResponse<Map> response) {
        verifyR01Response(response)
    }

    @Override
    void verifyN01Response(HttpResponse<Map> response) {
        verifyR01Response(response)
    }

    @Override
    void verifyR04KnownIdResponse(HttpResponse<Map> response, String id) {
        verifyNotFound(response, id)
    }

    @Override
    void verifyR05KnownIdResponse(HttpResponse<Map> response, String id) {
        verifyNotFound(response, id)
    }

    @Override
    List<String> getPermanentGroupNames() {
        ['applicationAdmin', 'administrators', 'readers', 'editors']
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[.+?@.+?] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[.+?@.+?] changed properties \[description]/
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
    "delete",
    "show",
    "update"
  ]
}'''
    }

}

