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

import uk.ac.ox.softeng.maurodatamapper.core.async.DomainExport
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * Users:
 *
 * admin : admin
 * editor : rw on both DataModels
 * reader1 : rw on 'Complex Test DataModel' and r on 'Simple Test DataModel'
 * reader2 : r on 'Complex Test DataModel' and rw on 'Simple Test DataModel'
 * reader3 : r on 'Complex Test DataModel' no access to 'Simple Test DataModel'
 *
 */
@Slf4j
abstract class FunctionalSpec extends BaseFunctionalSpec implements SecurityDefinition {

    //    @RunOnce
    //    @Transactional
    //    def setup() {
    //        log.debug('Check and setup test data')
    //        // Install all required domain data here
    //    }
    //
    //    @Transactional
    //    def cleanupSpec() {
    //        // Remove all installed required domains here
    //        cleanUpResource()
    //        cleanUpResources()
    //    }

    @Transactional
    def cleanup() {
        logout()
        DomainExport.deleteAll(DomainExport.list())
    }

    @Override
    void cleanUpData(String id = null) {
        //log.warn('Do not use cleanUpData')
    }

    @Autowired
    GroupRoleService groupRoleService

    Expectations getExpectations() {
        Expectations.builder().withDefaultExpectations()
    }

    @Transactional
    CatalogueUser getUserByEmailAddress(String emailAddress) {
        CatalogueUser.findByEmailAddress(emailAddress)
    }

    @Transactional
    CatalogueUser getUserById(String id) {
        CatalogueUser.get(id)
    }

    @Transactional
    UserGroup getUserGroup(String name) {
        UserGroup.findByName(name)
    }

    @Transactional
    String getUserGroupId(String name) {
        UserGroup.findByName(name).id.toString()
    }

    GroupRole getGroupRole(String name) {
        groupRoleService.getFromCache(name).groupRole
    }

    HttpResponse<Map> loginAdmin() {
        loginUser(userEmailAddresses.admin, 'password')
    }

    HttpResponse<Map> loginEditor() {
        loginUser(userEmailAddresses.editor, 'password')
    }

    HttpResponse<Map> loginContainerAdmin() {
        loginUser(userEmailAddresses.containerAdmin, 'password')
    }

    HttpResponse<Map> loginReader() {
        CatalogueUser user = getUserByEmailAddress(userEmailAddresses.reader)
        loginUser(user.emailAddress, user.tempPassword)
    }

    HttpResponse<Map> loginReviewer() {
        CatalogueUser user = getUserByEmailAddress(userEmailAddresses.reviewer)
        loginUser(user.emailAddress, user.tempPassword)
    }

    HttpResponse<Map> loginAuthor() {
        CatalogueUser user = getUserByEmailAddress(userEmailAddresses.author)
        loginUser(user.emailAddress, user.tempPassword)
    }

    HttpResponse<Map> loginCreator() {
        CatalogueUser user = getUserByEmailAddress(userEmailAddresses.creator)
        loginUser(user.emailAddress, user.tempPassword)
    }

    HttpResponse<Map> loginAuthenticated() {
        CatalogueUser user = getUserByEmailAddress(userEmailAddresses.authenticated)
        loginUser(user.emailAddress, user.tempPassword)
    }

    HttpResponse<Map> loginAuthenticated2() {
        CatalogueUser user = getUserByEmailAddress(userEmailAddresses.authenticated2)
        loginUser(user.emailAddress, user.tempPassword)
    }

    HttpResponse<Map> loginUser(String id) {
        CatalogueUser user = getUserById(id) ?: getUserByEmailAddress(id)
        loginUser(user.emailAddress, user.tempPassword ?: 'password')
    }

    HttpResponse<Map> loginUser(String emailAddress, String userPassword) {
        logout()
        log.trace('Logging in as {}', emailAddress)
        POST('authentication/login', [
            username: emailAddress,
            password: userPassword
        ], MAP_ARG, true)
        verifyResponse(OK, response)
        response
    }

    HttpResponse<Map> login(String name) {
        if (name && name != 'Anonymous') invokeMethod("login${name}", null) as HttpResponse<Map>
        else logout()
    }

    void logout() {
        log.trace('Logging out')
        GET('authentication/logout', MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        currentCookie = null
    }

    void verifyForbidden(HttpResponse<Map> response) {
        verifyResponse HttpStatus.FORBIDDEN, response
        assert response.body().additional
    }

    void verifyNotFound(HttpResponse<Map> response, def id) {
        verifyResponse HttpStatus.NOT_FOUND, response
        assert response.body().resource
        assert response.body().path
        if (id) assert response.body().id == id
    }

    @Transactional
    void addAccessShares(String id, String resourceUrl = '') {
        log.warn('Access is not inherited so adding permissions directly')
        addShare(id, 'readers', GroupRole.READER_ROLE_NAME, resourceUrl)
        addShare(id, 'reviewers', GroupRole.REVIEWER_ROLE_NAME, resourceUrl)
        addShare(id, 'authors', GroupRole.AUTHOR_ROLE_NAME, resourceUrl)
        addShare(id, 'editors', GroupRole.EDITOR_ROLE_NAME, resourceUrl)
        addShare(id, 'containerAdmins', GroupRole.CONTAINER_ADMIN_ROLE_NAME, resourceUrl)
    }

    @Transactional
    void removeAccessShares(String id, String resourceUrl = '') {
        log.info('Remove reader share from {}', id)
        loginAdmin()
        removeShare(id, 'readers', GroupRole.READER_ROLE_NAME, resourceUrl)
        removeShare(id, 'reviewers', GroupRole.REVIEWER_ROLE_NAME, resourceUrl)
        removeShare(id, 'authors', GroupRole.AUTHOR_ROLE_NAME, resourceUrl)
        removeShare(id, 'editors', GroupRole.EDITOR_ROLE_NAME, resourceUrl)
        removeShare(id, 'containerAdmins', GroupRole.CONTAINER_ADMIN_ROLE_NAME, resourceUrl)
        logout()
    }

    @Transactional
    void addShare(String id, String groupName, String role, String resourceUrl = '') {
        log.info('Add {} share to {}', role, id)
        String groupRoleId = getGroupRole(role).id.toString()
        String groupId = getUserGroup(groupName).id.toString()
        String endpoint = "${resourceUrl}$id/groupRoles/$groupRoleId/userGroups/$groupId"
        POST(endpoint, [:])
        verifyResponse CREATED, response
    }

    @Transactional
    void removeShare(String id, String groupName, String role, String resourceUrl = '') {
        log.info('Remove {} share from {}', role, id)
        String groupRoleId = getGroupRole(role).id.toString()
        String groupId = getUserGroup(groupName).id.toString()
        String endpoint = "${resourceUrl}$id/groupRoles/$groupRoleId/userGroups/$groupId"
        DELETE(endpoint, [:])
        verifyResponse CREATED, response
    }

    @Override
    CatalogueUser getAdmin() {
        throw new IllegalStateException('User is not available in this context, use getUserByEmailAddress(userEmailAddresses.admin)')
    }

    @Override
    CatalogueUser getEditor() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.editor)')
    }

    @Override
    CatalogueUser getPending() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.pending)')
    }

    @Override
    CatalogueUser getContainerAdmin() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.containerAdmin)')
    }

    @Override
    CatalogueUser getAuthor() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.author)')
    }

    @Override
    CatalogueUser getReviewer() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.reviewer)')
    }

    @Override
    CatalogueUser getReader() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.reader)')
    }

    @Override
    CatalogueUser getAuthenticated() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.authenticated)')
    }

    @Override
    UserGroup getAdmins() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.reader2)')
    }

    @Override
    UserGroup getEditors() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.reader3)')
    }

    @Override
    UserGroup getReaders() {
        throw new IllegalStateException('User is not available in this context, getUserByEmailAddress(userEmailAddresses.reader4)')
    }
}
