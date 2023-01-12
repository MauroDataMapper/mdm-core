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
package uk.ac.ox.softeng.maurodatamapper.testing.functional

import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.apache.commons.lang3.NotImplementedException
import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired

import static io.micronaut.http.HttpStatus.CREATED
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
@Slf4j
abstract class UserAccessWithoutUpdatingFunctionalSpec extends ReadOnlyUserAccessFunctionalSpec {

    @Autowired(required = false)
    List<SecurableResourceService> securableResourceServices

    abstract Map getValidJson()

    abstract Map getInvalidJson()

    String getSavePath() {
        getResourcePath()
    }

    void verifyDefaultCreationResponse(HttpResponse<Map> response, int count) {
        assert !expectations.hasDefaultCreation
    }

    void verifyNA04Response(HttpResponse<Map> response, String id) {
        verifyNotFound response, id
    }

    void verify03ValidResponseBody(HttpResponse<Map> response) {
        assert responseBody().id
        validJson.each {k, v ->
            if (v instanceof Map) {
                v.each {k1, v1 ->
                    assert responseBody()[k][k1] == v1
                }
            } else {
                assert responseBody()[k] == v
            }
        }
    }

    void verify03CannotCreateResponse(HttpResponse<Map> response, String name) {
        if ((expectations.can(name, 'see') && expectations.accessPermissionIsInherited) ||
            (!expectations.can(name, 'create') && expectations.isSecuredResource)) verifyForbidden(response)
        else verifyNotFound response, null
    }

    void verify04UnknownIdResponse(HttpResponse<Map> response, String name, String id) {
        if (expectations.can(name, 'delete')) {
            verifyNotFound(response, id)
        } else if (expectations.can(name, 'see') && !expectations.isSecuredResource) verifyForbidden(response)
        else verifyNotFound(response, id)
    }

    void verify04NotAllowedToDeleteResponse(HttpResponse<Map> response, String name, String id) {
        if (expectations.can(name, 'see')) verifyForbidden response
        else verifyNotFound(response, id)
    }

    void verifySameValidDataCreationResponse() {
        verifyResponse UNPROCESSABLE_ENTITY, response
        assert responseBody().total == 1
        assert responseBody().errors.first().message
    }

    void verifySoftDeleteResponse(HttpResponse<Map> response) {
        verifyResponse OK, response
        assert responseBody().deleted == true
    }

    void verifyDeleteResponse(HttpResponse<Map> response) {
        verifyResponse NO_CONTENT, response
    }

    /**
     * Items are created by the editor user
     * This ensures that they dont have some possible weird admin protection
     * @return
     */
    @Override
    String getValidId() {
        loginCreator()
        POST(getSavePath(), validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = responseBody().id
        addAccessShares(id)
        logout()
        id
    }

    @Override
    void addAccessShares(String id, String resourceUrl = '') {
        if (expectations.accessPermissionIsNotInherited) {
            super.addAccessShares(id, resourceUrl)
        }
    }

    @Override
    @Transactional
    void removeValidIdObject(String id) {
        removeValidIdObject(id, NO_CONTENT)
    }

    void removeValidIdObject(String id, HttpStatus expectedStatus) {
        if (!id) return
        log.info('Removing valid id {}', id)
        if (expectations.isSoftDeleteByDefault) {
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
        loginCreator()
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
        SecurableResourceGroupRole.bySecurableResourceIds(ids.collect {Utils.toUuid(it)}).deleteAll()
        safeSessionFlush()
        cleanupUserGroups()
    }

    @Transactional
    void cleanupUserGroups() {
        Number groupsLeftOver = UserGroup.byNameNotInList(getPermanentGroupNames()).count()
        if (groupsLeftOver) {
            log.info('Cleaning up groups, {} user groups still remain. Ignoring groups {},', groupsLeftOver, getPermanentGroupNames())
            List<UserGroup> groupsToDelete = UserGroup.byNameNotInList(getPermanentGroupNames()).list()

            // Seems to be possible in the UserGroupFS to get a result in groupsLeftOver which is then empty on the second call, possibly a timing issue
            if (groupsToDelete*.id) {
                // This is purely here to provide info about roles and resources which havent been cleaned up
                // It should not be used to perform cleanp of these roles and resources
                List<SecurableResourceGroupRole> rolesLeftOver = SecurableResourceGroupRole.byUserGroupIds(groupsToDelete*.id).list()

                if (rolesLeftOver) {
                    log.warn('Roles not cleaned up : {}', rolesLeftOver.size())
                    cleanupOrphanedRoles(rolesLeftOver)
                }
            }
            UserGroup.byNameNotInList(getPermanentGroupNames()).deleteAll()
        }

        safeSessionFlush()
        assert UserGroup.count() == getPermanentGroupNames().size()
    }

    void cleanupOrphanedRoles(List<SecurableResourceGroupRole> rolesLeftOver) {

        rolesLeftOver.each {srgr ->
            log.warn('Left over role resource {}:{}:{}:{}', srgr.groupRole.name, srgr.userGroup.name, srgr.securableResourceDomainType, srgr.securableResourceId)
            SecurableResourceService service = securableResourceServices.find {it.handles(srgr.securableResourceDomainType)}

            if (!service) {
                Assert.fail('Roles remaining these need to be cleaned up from another test and cannot remote clean them as no service to handle securable resource.' +
                            '\nSee logs to find out what roles and resources havent been cleaned')
            }
            SecurableResource resource = service.get(srgr.securableResourceId)
            if (resource) {
                log.warn('Resource {}:{} was not cleaned up', resource.domainType, resource.resourceId)
                service.delete(resource)
            }
        }
        SecurableResourceGroupRole.deleteAll(rolesLeftOver)
        safeSessionFlush()
    }

    List<String> getPermanentGroupNames() {
        ['administrators', 'readers', 'editors', 'reviewers', 'authors', 'containerAdmins']
    }

    void 'CORE-#prefix-03 : Test the save action correctly persists an instance (as #name)'() {
        given:
        login(name)


        if (expectations.hasDefaultCreation && expectations.can(name, 'create')) {
            when: 'The save action is executed with no content'
            POST(getSavePath(), [:], MAP_ARG, true)

            then:
            verifyResponse CREATED, response
            assert responseBody().id
            verifyDefaultCreationResponse(response, 0)

            and:
            removeValidIdObject(responseBody().id)

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verifyResponse CREATED, response
            assert responseBody().id
            verifyDefaultCreationResponse(response, 1)

            and:
            removeValidIdObject(responseBody().id)

            when: 'The save action is executed with valid data'
            POST(getSavePath(), validJson, MAP_ARG, true)

            then:
            verifyResponse CREATED, response
            verify03ValidResponseBody response

            cleanup:
            removeValidIdObject(responseBody().id)

        } else if (expectations.can(name, 'create')) {
            when: 'The save action is executed with no content'
            POST(getSavePath(), [:], MAP_ARG, true)

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
            verify03ValidResponseBody response

            cleanup:
            removeValidIdObject(responseBody().id)

        } else {
            when: 'The save action is executed with no content'
            POST(getSavePath(), [:], MAP_ARG, true)

            then:
            verify03CannotCreateResponse(response, name)

            when: 'The save action is executed with invalid data'
            POST(getSavePath(), invalidJson, MAP_ARG, true)

            then: 'The response is correct'
            verify03CannotCreateResponse(response, name)

            when: 'The save action is executed with valid data'
            POST(getSavePath(), validJson, MAP_ARG, true)

            then:
            verify03CannotCreateResponse(response, name)
        }

        where:
        prefix | name
        'LO'   | 'Anonymous'
        'NA'   | 'Authenticated'
        'RE'   | 'Reader'
        'RV'   | 'Reviewer'
        'AU'   | 'Author'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'CORE-#prefix-04 : Test the delete action correctly deletes an instance (as #name)'() {
        given:
        def id = getValidId()
        String rId = UUID.randomUUID().toString()
        login(name)

        when: 'When the delete action is executed on an unknown instance'
        DELETE("${rId}")

        then: 'The response is correct'
        verify04UnknownIdResponse response, name, rId

        when: 'When the delete action is executed on an existing instance'
        DELETE("$id")

        then: 'The response is correct'
        if (expectations.can(name, 'delete')) {
            if (expectations.isSoftDeleteByDefault) {
                verifySoftDeleteResponse(response)
            } else {
                verifyDeleteResponse response
            }
        } else verify04NotAllowedToDeleteResponse(response, name, id)

        if (expectations.can(name, 'delete') && expectations.isSoftDeleteByDefault) {
            when: 'permanent delete is performed'
            DELETE("$id?permanent=true")

            then:
            if (name == 'Editor') verifyForbidden(response)
            else verifyDeleteResponse response
        }

        cleanup:
        if (!expectations.can(name, 'delete')) removeValidIdObject(id)
        else {
            loginCreator()
            GET(id)
            if (response.status() == OK) {
                removeValidIdObject(id)
            }
        }

        where:
        prefix | name
        'LO'   | 'Anonymous'
        'NA'   | 'Authenticated'
        'RE'   | 'Reader'
        'RV'   | 'Reviewer'
        'AU'   | 'Author'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }
}
