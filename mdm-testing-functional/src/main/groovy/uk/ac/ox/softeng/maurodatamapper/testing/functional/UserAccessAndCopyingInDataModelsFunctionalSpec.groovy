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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityUtils
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * Tests the copy endpoints for the resource and the semantic link endpoint has links for the resources
 *
 * <pre>
 * Controller: dataXxxxx
 *  |  POST    |  /api/dataModels/${dataModelId}/dataXxxxx/${otherDataModelId}/${otherDataXxxxId}  | Action: copyDataXxxxx
 *
 * Controller: semanticLink
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks  | Action: index
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController* @since 18/05/2018
 */
@Slf4j
abstract class UserAccessAndCopyingInDataModelsFunctionalSpec extends UserAccessFunctionalSpec {

    abstract void verifyCopiedResponseBody(HttpResponse<Map> response)

    abstract String getExpectedTargetId()

    String getCatalogueItemDomainType() {
        getEditsPath()
    }

    String getCopyPath(String fromId) {
        "dataModels/${getSimpleDataModelId()}/${getCatalogueItemDomainType()}/${getComplexDataModelId()}/${fromId}"
    }

    String getAlternativePath(String id) {
        "dataModels/${getSimpleDataModelId()}/${getCatalogueItemDomainType()}/$id"
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Transactional
    String getSimpleDataModelId() {
        DataModel.findByLabel('Simple Test DataModel').id.toString()
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'update')
            .whereEditorsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'update')
            .whereAuthorsCanAction('comment', 'editDescription', 'show',)
            .whereReviewersCanAction('comment', 'show')
            .whereReadersCanAction('show')
    }

    @RunOnce
    @Transactional
    def setup() {
        log.info('Add group with new authenticated user')
        CatalogueUser authenticated2 = new CatalogueUser(emailAddress: userEmailAddresses.authenticated2,
                                                         firstName: 'authenticated2', lastName: 'User',
                                                         createdBy: userEmailAddresses.functionalTest,
                                                         tempPassword: SecurityUtils.generateRandomPassword())
        // To allow testing of reader group rights which reader2 is not in
        new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'copyPermissionsGroup',
            ).addToGroupMembers(authenticated2).save(flush: true)
    }

    @Transactional
    def addEditorReaderPermission() {
        UserGroup group = getUserGroup('copyPermissionsGroup')
        new SecurableResourceGroupRole(securableResource: DataModel.findByLabel('Simple Test DataModel'),
                                       userGroup: group,
                                       groupRole: GroupRole.findByName(GroupRole.EDITOR_ROLE_NAME),
                                       createdBy: userEmailAddresses.functionalTest
        ).save(flush: true)
        new SecurableResourceGroupRole(securableResource: DataModel.findByLabel('Complex Test DataModel'),
                                       userGroup: group,
                                       groupRole: GroupRole.findByName(GroupRole.READER_ROLE_NAME),
                                       createdBy: userEmailAddresses.functionalTest
        ).save(flush: true)
    }

    @Transactional
    def removeEditorReaderPermission() {
        SecurableResourceGroupRole.deleteAll(SecurableResourceGroupRole.findAllByUserGroup(getUserGroup('copyPermissionsGroup')))
    }

    @Transactional
    def cleanupSpec() {
        log.info('Removing group with new authenticated user')
        UserGroup.findByName('copyPermissionsGroup').delete(flush: true)
        CatalogueUser.findByEmailAddress(userEmailAddresses.authenticated2).delete(flush: true)
    }

    @Override
    void removeValidIdObjectUsingApi(String id, HttpStatus expectedStatus) {
        log.info('Removing valid id {} using DELETE', id)
        loginAdmin()
        DELETE(id)
        if (response.status() == NOT_FOUND) {
            DELETE(getAlternativePath(id), MAP_ARG, true)
        }
        verifyResponse expectedStatus, response
        logout()
    }

    void cleanupCopiedItem(String id) {
        removeValidIdObject(id)
    }

    void checkSimpleDataModelIsClean() {
        String dmId = getSimpleDataModelId()
        loginEditor()
        GET("dataModels/${dmId}/dataTypes", MAP_ARG, true)
        assert responseBody().count == 0
        GET("dataModels/${dmId}/dataClasses", MAP_ARG, true)
        assert responseBody().count == 1
        String dcId = responseBody().items.first().id
        GET("dataModels/${dmId}/dataClasses/${dcId}/dataClasses", MAP_ARG, true)
        assert responseBody().count == 0
        GET("dataModels/${dmId}/dataClasses/${dcId}/dataElements", MAP_ARG, true)
        assert responseBody().count == 0
    }

    @Override
    List<String> getPermanentGroupNames() {
        super.getPermanentGroupNames() + ['copyPermissionsGroup']
    }

    List<String> getEditorModelItemAvailableActions() {
        ['show', 'comment', 'editDescription', 'update', 'save', 'delete']
    }

    void 'C01 : test copying (as writer to both DataModels)'() {
        given:
        loginEditor()

        when: 'trying to copy non-existent'
        String randomId = UUID.randomUUID().toString()
        POST(getCopyPath(randomId), [:], MAP_ARG, true)

        then:
        verifyNotFound response, randomId

        when: 'trying to copy valid'
        POST(getCopyPath(getExpectedTargetId()), [:], MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String id = response.body().id
        verifyCopiedResponseBody response

        when: 'verify the id exists'
        GET(getAlternativePath(id), MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().id == id

        when:
        GET("${getCatalogueItemDomainType()}/${id}/semanticLinks", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count == 1
        response.body().items.first().domainType == 'SemanticLink'
        response.body().items.first().linkType == 'Refines'
        response.body().items.first().sourceMultiFacetAwareItem.id == id
        response.body().items.first().targetMultiFacetAwareItem.id == getExpectedTargetId()
        response.body().items.first().sourceMultiFacetAwareItem.domainType == response.body().items.first().targetMultiFacetAwareItem.domainType

        cleanup:
        cleanupCopiedItem(id)
        checkSimpleDataModelIsClean()
    }

    void 'C02 : test copying (as writer of TO DataModel and reader of FROM DataModel)'() {
        given:
        addEditorReaderPermission()
        loginAuthenticated2()

        when: 'trying to copy non-existent'
        String randomId = UUID.randomUUID().toString()
        POST(getCopyPath(randomId), [:], MAP_ARG, true)

        then:
        verifyNotFound response, randomId

        when: 'trying to copy valid'
        POST(getCopyPath(getExpectedTargetId()), [:], MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String id = response.body().id
        verifyCopiedResponseBody response

        when:
        GET("${getCatalogueItemDomainType()}/${id}/semanticLinks", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count == 1
        response.body().items.first().domainType == 'SemanticLink'
        response.body().items.first().linkType == 'Refines'
        response.body().items.first().sourceMultiFacetAwareItem.id == id
        response.body().items.first().targetMultiFacetAwareItem.id == getExpectedTargetId()
        response.body().items.first().sourceMultiFacetAwareItem.domainType == response.body().items.first().targetMultiFacetAwareItem.domainType

        cleanup:
        removeEditorReaderPermission()
        cleanupCopiedItem(id)
        checkSimpleDataModelIsClean()
    }

    void 'C03 : test copying (as reader of TO DataModel and writer of FROM DataModel)'() {
        given:
        loginReader()

        when: 'trying to copy non-existent'
        String id = UUID.randomUUID().toString()
        POST(getCopyPath(id), [:], MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'trying to copy valid'
        POST(getCopyPath(getExpectedTargetId()), [:], MAP_ARG, true)

        then:
        verifyForbidden response
    }

    void 'C04 : test copying (as authenticated user)'() {
        given:
        loginAuthenticated()

        when: 'trying to copy non-existent'
        String id = UUID.randomUUID().toString()
        POST(getCopyPath(id), [:], MAP_ARG, true)

        then:
        verifyNotFound response, getSimpleDataModelId()

        when: 'trying to copy valid'
        id = getExpectedTargetId()
        POST(getCopyPath(id), [:], MAP_ARG, true)

        then:
        verifyNotFound response, getSimpleDataModelId()
    }
}
