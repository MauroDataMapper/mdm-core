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
package uk.ac.ox.softeng.maurodatamapper.testing.functional

import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.apache.commons.lang3.NotImplementedException
import org.junit.Assert
import spock.lang.Stepwise

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * Tests the below endpoints for editor/reader/not logged/admin/no access in access
 * <pre>
 *  | POST   | /api/${resourcePath}       | Action: save   |
 *  | DELETE | /api/${resourcePath}/${id} | Action: delete |
 *  | GET    | /api/${resourcePath}       | Action: index  | [inherited test]
 *  | GET    | /api/${resourcePath}/${id} | Action: show   | [inherited test]
 * </pre>
 */
@Stepwise
@Slf4j
abstract class UserAccessWithoutUpdatingFunctionalSpec extends ReadOnlyUserAccessFunctionalSpec {

    abstract Map getValidJson()

    abstract Map getInvalidJson()

    String getSavePath() {
        getResourcePath()
    }

    Map getNoContent() {
        [:]
    }

    HttpStatus getUnknownIdDeletedStatus() {
        NOT_FOUND
    }

    Boolean getReaderCanCreate() {
        false
    }

    Boolean getAuthenticatedUsersCanCreate() {
        false
    }

    Boolean isDisabledNotDeleted() {
        false
    }

    Boolean hasDefaultCreation() {
        false
    }

    Boolean getReaderCanSeeEditorCreatedItems() {
        true
    }

    Boolean readerPermissionIsInherited() {
        false
    }

    void verifyDefaultCreationResponse(HttpResponse<Map> response, int count) {
        assert !hasDefaultCreation()
    }

    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyR03NoContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyR03InvalidContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyR03ValidContentResponse(HttpResponse<Map> response) {
        verifyForbidden response
    }

    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyNotFound response, id
    }

    void verifyR04KnownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
    }

    void verifyE03ValidResponseBody(HttpResponse<Map> response) {
        assert response.body().id
        validJson.each { k, v ->
            if (v instanceof Map) {
                v.each { k1, v1 ->
                    assert response.body()[k][k1] == v1
                }
            } else {
                assert response.body()[k] == v
            }
        }
    }

    void verifySameValidDataCreationResponse() {
        verifyResponse UNPROCESSABLE_ENTITY, response
        assert response.body().total == 1
        assert response.body().errors.first().message
    }

    /**
     * Items are created by the editor user
     * This ensures that they dont have some possible weird admin protection
     * @return
     */
    @Override
    String getValidId() {
        loginEditor()
        POST(getSavePath(), validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        addReaderShare(id)
        logout()
        id
    }

    @Transactional
    void addReaderShare(String id) {
        if (!readerPermissionIsInherited()) {
            log.info('Add reader share to {}', id)
            String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
            String readerGroupId = getUserGroup('validIdGroup').id.toString()
            String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
            POST(endpoint, [:])
            verifyResponse CREATED, response
        }
    }

    @Transactional
    void removeReaderShare(String id) {
        log.info('Remove reader share from {}', id)
        loginAdmin()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('validIdGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        DELETE(endpoint, [:])
        verifyResponse NO_CONTENT, response
        logout()
    }

    @OnceBefore
    @Transactional
    def addValidIdReaderGroup() {
        log.info('Add group with new reader user')
        CatalogueUser reader = getUserByEmailAddress(userEmailAddresses.reader)
        // To allow testing of reader group rights which reader2 is not in
        UserGroup group = new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'validIdGroup',
            ).addToGroupMembers(reader)
        checkAndSave(messageSource, group)
    }

    @Transactional
    def cleanupSpec() {
        log.info('Removing valid id group')
        UserGroup.findByName('validIdGroup')?.delete(flush: true)
    }

    @Override
    @Transactional
    void removeValidIdObject(String id) {
        removeValidIdObject(id, NO_CONTENT)
    }

    void removeValidIdObject(String id, HttpStatus expectedStatus) {
        if (!id) return
        log.info('Removing valid id {}', id)
        if (isDisabledNotDeleted()) {
            removeValidIdObjectUsingTransaction(id)
        } else {
            removeValidIdObjectUsingApi(id, expectedStatus)
        }
        cleanUpRoles(id)
    }

    void removeValidIdObjectUsingTransaction(String id) {
        throw new NotImplementedException('removeValidIdObjectUsingTransaction')
    }

    void removeValidIdObjectUsingApi(String id, HttpStatus expectedStatus) {
        log.info('Removing valid id {} using DELETE', id)
        loginAdmin()
        DELETE(id)
        verifyResponse expectedStatus, response
        logout()
    }

    @Transactional
    void cleanUpRoles(String... ids) {
        cleanUpRoles(ids.toList())
    }

    @Transactional
    void cleanUpRoles(Collection<String> ids) {
        log.info('Cleaning up roles and groups')
        log.debug('Cleaning up {} roles for ids {}', SecurableResourceGroupRole.count(), ids)
        SecurableResourceGroupRole.bySecurableResourceIds(ids.collect { Utils.toUuid(it) }).deleteAll()
        sessionFactory.currentSession.flush()
        cleanupUserGroups()
    }

    @Transactional
    void cleanupUserGroups() {
        Number groupsLeftOver = UserGroup.byNameNotInList(getPermanentGroupNames()).count()
        if (groupsLeftOver) {
            log.info('Cleaning up groups, {} user groups still remain. Ignoring groups {},', groupsLeftOver, getPermanentGroupNames())
            List<UserGroup> groupsToDelete = UserGroup.byNameNotInList(getPermanentGroupNames()).list()

            // This is purely here to provide info about roles and resources which havent been cleaned up
            // It should not be used to perform cleanp of these roles and resources
            List<SecurableResourceGroupRole> rolesLeftOver = SecurableResourceGroupRole.byUserGroupIds(groupsToDelete*.id).list()
            if (rolesLeftOver) {
                log.warn('Roles not cleaned up : {}', rolesLeftOver.size())
                rolesLeftOver.each { role ->
                    log.warn('Left over role resource {}:{}:{}:{}', role.groupRole.name, role.userGroup.name, role.securableResourceDomainType, role.securableResourceId)
                }
                Assert.fail('Roles remaining these need to be cleaned up from another test.' +
                            '\nSee logs to find out what roles and resources havent been cleaned')
            }
            UserGroup.byNameNotInList(getPermanentGroupNames()).deleteAll()
        }

        sessionFactory.currentSession.flush()
        assert UserGroup.count() == getPermanentGroupNames().size()
    }

    List<String> getPermanentGroupNames() {
        ['validIdGroup', 'administrators', 'readers', 'editors']
    }

    /*
     * Logged in as editor testing
     */

    void 'E03 : Test the save action correctly persists an instance (as editor)'() {
        given:
        loginEditor()

        //        if (hasInvalidOption()) {

        if (hasDefaultCreation()) {
            when: 'The save action is executed with no content'
            POST(getSavePath(), noContent, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse CREATED, response
            response.body().id
            verifyDefaultCreationResponse(response, 0)

            and:
            removeValidIdObject(response.body().id)

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse CREATED, response
            response.body().id
            verifyDefaultCreationResponse(response, 1)

            and:
            removeValidIdObject(response.body().id)

        } else {
            when: 'The save action is executed with no content'
            POST(getSavePath(), noContent, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse UNPROCESSABLE_ENTITY, response

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse UNPROCESSABLE_ENTITY, response
        }
        //        }

        when: 'The save action is executed with valid data'
        POST(getSavePath(), validJson, MAP_ARG, true)

        then: 'The response is correct'
        verifyResponse CREATED, response
        verifyE03ValidResponseBody(response)

        cleanup:
        removeValidIdObject(response.body().id)

    }

    void 'E04 : Test the delete action correctly deletes an instance (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()

        when: 'When the delete action is executed on an unknown instance'
        DELETE("${UUID.randomUUID()}")

        then: 'The response is correct'
        verifyResponse getUnknownIdDeletedStatus(), response

        when: 'When the delete action is executed on an existing instance'
        DELETE("$id")

        then: 'The response is correct'
        if (isDisabledNotDeleted()) {
            verifyResponse OK, response
            response.body().disabled == true
        } else {
            verifyResponse NO_CONTENT, response
        }

        cleanup:
        removeValidIdObject(id, NOT_FOUND)
    }

    /*
     * Logged out testing
     */

    void 'L03 : Test the save action correctly persists an instance (not logged in)'() {
        when: 'The save action is executed with no content'
        POST(getSavePath(), noContent, MAP_ARG, true)

        then: 'The response is correct'
        verifyL03NoContentResponse response

        when: 'The save action is executed with invalid data'
        POST(getSavePath(), invalidJson, MAP_ARG, true)

        then: 'The response is correct'
        verifyL03InvalidContentResponse response

        when: 'The save action is executed with valid data'
        POST(getSavePath(), validJson, MAP_ARG, true)

        then: 'The response is correct'
        verifyL03ValidContentResponse response
    }

    void 'L04 : Test the delete action correctly deletes an instance (not logged in)'() {
        given:
        def id = getValidId()
        String rId = UUID.randomUUID().toString()

        when: 'When the delete action is executed on an unknown instance'
        DELETE("${rId}")

        then: 'The response is correct'
        verifyNotFound response, rId

        when: 'When the delete action is executed on an existing instance'
        DELETE("$id")

        then: 'The response is correct'
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */

    void 'N03 : Test the save action correctly persists an instance (as no access/authenticated)'() {
        given:
        loginAuthenticated()


        if (hasDefaultCreation() && getAuthenticatedUsersCanCreate()) {
            when: 'The save action is executed with no content'
            POST(getSavePath(), noContent, MAP_ARG, true)

            then:
            verifyResponse CREATED, response
            response.body().id
            verifyDefaultCreationResponse(response, 0)

            and:
            removeValidIdObject(response.body().id)

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse CREATED, response
            response.body().id
            verifyDefaultCreationResponse(response, 1)

            and:
            removeValidIdObject(response.body().id)

            when: 'The save action is executed with valid data'
            POST(getSavePath(), validJson, MAP_ARG, true)

            then:
            verifyResponse CREATED, response
            response.body().id
            validJson.each {k, v ->
                assert response.body()[k] == v
            }

            and:
            removeValidIdObject(response.body().id)

        } else if (getAuthenticatedUsersCanCreate()) {
            when: 'The save action is executed with no content'
            POST(getSavePath(), noContent, MAP_ARG, true)

            then:
            verifyResponse UNPROCESSABLE_ENTITY, response

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse UNPROCESSABLE_ENTITY, response

            when: 'The save action is executed with valid data'
            POST(getSavePath(), validJson, MAP_ARG, true)

            then:
            verifyResponse CREATED, response
            response.body().id
            validJson.each {k, v ->
                assert response.body()[k] == v
            }

            and:
            removeValidIdObject(response.body().id)

        } else {
            when: 'The save action is executed with no content'
            POST(getSavePath(), noContent, MAP_ARG, true)

            then:
            verifyN03NoContentResponse response

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyN03InvalidContentResponse response

            when: 'The save action is executed with valid data'
            POST(getSavePath(), validJson, MAP_ARG, true)

            then:
            verifyN03ValidContentResponse response
        }
    }

    void 'N04 : Test the delete action correctly deletes an instance (as no access/authenticated)'() {
        given:
        def id = getValidId()
        String rId = UUID.randomUUID().toString()
        loginAuthenticated()

        when: 'When the delete action is executed on an unknown instance'
        DELETE("${rId}")

        then: 'The response is correct'
        verifyNotFound response, rId

        when: 'When the delete action is executed on an existing instance'
        DELETE("$id")

        then: 'The response is correct'
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a reader only user
     */

    void 'R03 : Test the save action correctly persists an instance (as reader)'() {
        given:
        loginReader()

        if (hasDefaultCreation() && getReaderCanCreate()) {
            when: 'The save action is executed with no content'
            POST(getSavePath(), noContent, MAP_ARG, true)

            then:
            verifyResponse CREATED, response
            response.body().id
            verifyDefaultCreationResponse(response, 0)

            and:
            removeValidIdObject(response.body().id)

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse CREATED, response
            response.body().id
            verifyDefaultCreationResponse(response, 1)

            and:
            removeValidIdObject(response.body().id)

            when: 'The save action is executed with valid data'
            POST(getSavePath(), validJson, MAP_ARG, true)

            then:
            verifyResponse CREATED, response
            response.body().id
            validJson.each {k, v ->
                assert response.body()[k] == v
            }

            and:
            removeValidIdObject(response.body().id)

        } else if (getReaderCanCreate()) {
            when: 'The save action is executed with no content'
            POST(getSavePath(), noContent, MAP_ARG, true)

            then:
            verifyResponse UNPROCESSABLE_ENTITY, response

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse UNPROCESSABLE_ENTITY, response

            when: 'The save action is executed with valid data'
            POST(getSavePath(), validJson, MAP_ARG, true)

            then:
            verifyResponse CREATED, response
            response.body().id
            validJson.each {k, v ->
                assert response.body()[k] == v
            }

            and:
            removeValidIdObject(response.body().id)

        } else {
            when: 'The save action is executed with no content'
            POST(getSavePath(), noContent, MAP_ARG, true)

            then:
            verifyR03NoContentResponse(response)

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyR03InvalidContentResponse(response)

            when: 'The save action is executed with valid data'
            POST(getSavePath(), validJson, MAP_ARG, true)

            then:
            verifyR03ValidContentResponse(response)
        }
    }

    void 'R04 : Test the delete action correctly deletes an instance (as reader)'() {
        given:
        def id = getValidId()
        String rId = UUID.randomUUID().toString()
        loginReader()

        when: 'When the delete action is executed on an unknown instance'
        DELETE("${rId}")

        then: 'The response is correct'
        verifyR04UnknownIdResponse response, rId

        when: 'When the delete action is executed on an existing instance'
        DELETE("$id")

        then: 'The response is correct'
        verifyR04KnownIdResponse response, id

        cleanup:
        removeValidIdObject(id)
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */

    void 'A03 : Test the save action correctly persists an instance (as admin)'() {
        given:
        loginAdmin()

        when:
        POST(getSavePath(), validJson, MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        response.body().id

        when: 'Trying to save again using the same info'
        String id1 = response.body().id
        POST(getSavePath(), validJson, MAP_ARG, true)

        then:
        verifySameValidDataCreationResponse()
        String id2 = response.body()?.id

        cleanup:
        removeValidIdObject(id1)
        if (id2) {
            removeValidIdObject(id2) // not expecting anything, but just in case
        }
    }

    void 'A04 : Test the delete action correctly deletes an instance (as admin)'() {
        given:
        def id = getValidId()
        loginAdmin()

        when: 'When the delete action is executed on an unknown instance'
        DELETE("${UUID.randomUUID()}")

        then: 'The response is correct'
        verifyResponse unknownIdDeletedStatus, response

        when: 'When the delete action is executed on an existing instance'
        DELETE("$id")

        then: 'The response is correct'
        if (isDisabledNotDeleted()) {
            verifyResponse OK, response
            response.body().disabled == true
        } else {
            verifyResponse NO_CONTENT, response
        }

        cleanup:
        removeValidIdObject(id, NOT_FOUND)
    }

}
