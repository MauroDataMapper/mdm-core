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
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired

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

    //    @OnceBefore
    //    @Transactional
    //    def checkAndSetupData() {
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

    def cleanup() {
        logout()
    }

    @Override
    void cleanUpData(String id = null) {
        log.warn('Do not use cleanUpData')
    }

    @Autowired
    GroupRoleService groupRoleService

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

    HttpResponse<Map> loginReader() {
        CatalogueUser reader = getUserByEmailAddress(userEmailAddresses.reader)
        loginUser(reader.emailAddress, reader.tempPassword)
    }

    HttpResponse<Map> loginAuthenticated() {
        CatalogueUser reader = getUserByEmailAddress(userEmailAddresses.authenticated)
        loginUser(reader.emailAddress, reader.tempPassword)
    }

    HttpResponse<Map> loginAuthenticated2() {
        CatalogueUser reader = getUserByEmailAddress(userEmailAddresses.authenticated2)
        loginUser(reader.emailAddress, reader.tempPassword)
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
