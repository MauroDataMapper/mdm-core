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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.security.role

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import java.util.regex.Pattern

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: securableResourceGroupRole
 *  |  POST    | /api/userGroups/{id}/securableResourceGroupRoles         | Action: save
 *  |  GET     | /api/userGroups/{id}/securableResourceGroupRoles         | Action: index
 *  |  GET     | /api/userGroups/{id}/securableResourceGroupRoles/{id}    | Action: show
 *  |  DELETE  | /api/userGroups/{id}/securableResourceGroupRoles/{id}    | Action: delete
 *  |  PUT     | /api/userGroups/{id}/securableResourceGroupRoles/{id}    | Action: update
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRoleController
 */
@Integration
@Slf4j
class SecurableResourceGroupRoleFunctionalSpec extends UserAccessFunctionalSpec {

    GroupRoleService groupRoleService

    @Shared
    String groupAdminId

    @Shared
    String folderId

    @Shared
    authorGroupRoleId

    @Shared
    reviewerGroupRoleId

    @Shared
    applicationAdminGroupRoleId

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')

        // For now to allow existing permissions tests the editor user needs to be group admin role level
        // This will give editor the "editing" rights to usergroups
        UserGroup groupAdmin = new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'groupAdmins',
            applicationGroupRole: groupRoleService.getFromCache(GroupRole.GROUP_ADMIN_ROLE_NAME).groupRole)
            .addToGroupMembers(CatalogueUser.findByEmailAddress(userEmailAddresses.containerAdmin))
            .addToGroupMembers(CatalogueUser.findByEmailAddress(userEmailAddresses.editor))
        checkAndSave(messageSource, groupAdmin)

        groupAdminId = groupAdmin.id

        // For now to allow existing permissions tests the reader user needs to be container group admin role level
        // This will give editor the "reader" rights to usergroups
        UserGroup containerGroupAdmin = new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'containerGroupAdmins',
            applicationGroupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_GROUP_ADMIN_ROLE_NAME).groupRole)
            .addToGroupMembers(CatalogueUser.findByEmailAddress(userEmailAddresses.reader))
            .addToGroupMembers(CatalogueUser.findByEmailAddress(userEmailAddresses.reviewer))
            .addToGroupMembers(CatalogueUser.findByEmailAddress(userEmailAddresses.author))
        checkAndSave(messageSource, containerGroupAdmin)

        Folder folder = new Folder(label: 'Functional Test Folder', createdBy: userEmailAddresses.functionalTest);
        checkAndSave(messageSource, folder)
        folderId = folder.id

        authorGroupRoleId = groupRoleService.getFromCache(GroupRole.AUTHOR_ROLE_NAME).groupRole.id
        reviewerGroupRoleId = groupRoleService.getFromCache(GroupRole.REVIEWER_ROLE_NAME).groupRole.id
        applicationAdminGroupRoleId = groupRoleService.getFromCache(GroupRole.APPLICATION_ADMIN_ROLE_NAME).groupRole.id
    }

    @Transactional
    def cleanupSpec() {
        // Remove all installed required domains here
        UserGroup.findByName('groupAdmins').delete(flush: true)
        UserGroup.findByName('containerGroupAdmins').delete(flush: true)
        Folder.findById(folderId).delete(flush: true)
    }

    @Transactional
    String getUserGroupId(String userGroupName) {
        UserGroup userGroup = UserGroup.findByName(userGroupName)
        userGroup.id
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withoutEdits() //TODO Fix the edits endpoint
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .whereEditorsCannotChangePermissions()
            .whereReaders {
                canCreate()
            }
            .whereReviewers {
                canCreate()
            }
            .whereAuthors {
                canCreate()
                canSee()
                canIndex()
                cannotEditDescription()
                cannotUpdate()
            }
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'softDelete', 'update')
            .whereEditorsCanAction('show')
    }

    @Override
    void verify03ValidResponseBody(HttpResponse<Map> response) {
        assert response.body().id
    }

    @Override
    void verifyValidDataUpdateResponse(HttpResponse<Map> response, String id, Map update) {
        verifyResponse OK, response
    }

    @Override
    String getResourcePath() {
        "userGroups/${groupAdminId}/securableResourceGroupRoles"
    }

    // Note, this edits path does not work
    @Override
    String getEditsPath() {
        "folders/${folderId}"
    }

    @Override
    List<String> getPermanentGroupNames() {
        super.permanentGroupNames + ['groupAdmins', 'containerGroupAdmins']
    }

    Pattern getExpectedUpdateEditRegex() {
        ~/\[\w+( \w+)*:.+?] changed properties \[path, name]/
    }

    @Override
    Map getValidJson() {
        [
            "securableResourceDomainType": "Folder",
            "securableResourceId": folderId,
            "groupRole": [
                "id": authorGroupRoleId
            ]
        ]
    }

    // This is invalid because it is not allowed to set an application level group role
    @Override
    Map getInvalidJson() {
        [
            "securableResourceDomainType": "Folder",
            "securableResourceId": folderId,
            "groupRole": [
                "id": applicationAdminGroupRoleId
            ]
        ]
    }

    Map getValidNonDescriptionUpdateJson() {
        [
            "securableResourceDomainType": "Folder",
            "securableResourceId": folderId,
            "groupRole": [
                "id": reviewerGroupRoleId
            ]
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": []
}'''
    }

    @Override
    String getReaderIndexJson() {
        '''{
  "count": 0,
  "items": []
}'''
    }

    @Override
    String getShowJson() {
        '''{
   "availableActions": ["show"],
   "createdBy": "creator@test.com",
   "groupRole": {
       "displayName": "Author",
       "id": "${json-unit.matches:id}",
       "name": "author"
   },
   "id": "${json-unit.matches:id}",
   "securableResourceDomainType": "Folder",
   "securableResourceId": "${json-unit.matches:id}",
   "userGroup": {
       "id": "${json-unit.matches:id}",
       "name": "groupAdmins"
   }
 }'''
    }
}