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
package uk.ac.ox.softeng.maurodatamapper.security.utils

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole

import grails.util.Environment

trait SecurityDefinition {

    CatalogueUser admin
    CatalogueUser editor
    CatalogueUser pending

    CatalogueUser containerAdmin
    CatalogueUser author
    CatalogueUser reviewer
    CatalogueUser reader
    CatalogueUser authenticated

    UserGroup admins
    UserGroup editors
    UserGroup readers

    Map<String, String> userEmailAddresses = [
        admin          : StandardEmailAddress.ADMIN,

        unitTest       : StandardEmailAddress.UNIT_TEST,
        integrationTest: StandardEmailAddress.INTEGRATION_TEST,
        functionalTest : StandardEmailAddress.FUNCTIONAL_TEST,
        development    : StandardEmailAddress.DEVELOPMENT,

        pending        : 'pending@test.com',

        userAdmin      : 'user_admin@test.com',
        groupAdmin     : 'group_admin@test.com',

        containerAdmin : 'container_admin@test.com',
        editor         : 'editor@test.com',
        author         : 'author@test.com',
        reviewer       : 'reviewer@test.com',
        reader         : 'reader@test.com',
        authenticated  : 'authenticated@test.com',
        authenticated2 : 'authenticated2@test.com'
    ]

    CatalogueUser createAdminUser(String creatorKey) {
        admin = new CatalogueUser(emailAddress: userEmailAddresses.admin,
                                  firstName: 'Admin',
                                  lastName: 'User',
                                  organisation: 'Oxford BRC Informatics',
                                  jobTitle: 'God',
                                  createdBy: userEmailAddresses[creatorKey])
        if (Environment.current == Environment.PRODUCTION) {
            admin.setTempPassword('password')
        } else {
            admin.encryptAndSetPassword('password')
        }
        admin
    }

    UserGroup createAdminGroup(String creatorKey) {
        admins = new UserGroup(createdBy: userEmailAddresses[creatorKey],
                               name: 'administrators',
                               applicationGroupRole: GroupRole.findByName(GroupRole.SITE_ADMIN_ROLE_NAME),
                               undeleteable: true)
    }

    void createModernSecurityUsers(String creatorKey, boolean includeAdmin = true) {
        getOrCreateModernSecurityUsers(creatorKey, includeAdmin)
    }

    void createBasicGroups(String creatorKey, boolean includeAdmin = true) {
        getOrCreateBasicGroups(creatorKey, includeAdmin)
    }

    void getOrCreateModernSecurityUsers(String creatorKey, boolean includeAdmin = false) {
        if (includeAdmin) {
            admin = CatalogueUser.findByEmailAddress(userEmailAddresses.admin)
            if (!admin) createAdminUser(creatorKey)
        }
        containerAdmin = CatalogueUser.findByEmailAddress(userEmailAddresses.containerAdmin)
        editor = CatalogueUser.findByEmailAddress(userEmailAddresses.editor)
        pending = CatalogueUser.findByEmailAddress(userEmailAddresses.pending)
        author = CatalogueUser.findByEmailAddress(userEmailAddresses.author)
        reviewer = CatalogueUser.findByEmailAddress(userEmailAddresses.reviewer)
        reader = CatalogueUser.findByEmailAddress(userEmailAddresses.reader)
        authenticated = CatalogueUser.findByEmailAddress(userEmailAddresses.authenticated)
        if (!containerAdmin) {
            containerAdmin = new CatalogueUser(emailAddress: userEmailAddresses.containerAdmin,
                                               firstName: 'containerAdmin', lastName: 'User',
                                               createdBy: userEmailAddresses[creatorKey])
            containerAdmin.encryptAndSetPassword('password')
        }
        if (!editor) {
            editor = new CatalogueUser(emailAddress: userEmailAddresses.editor,
                                       firstName: 'editor', lastName: 'User',
                                       createdBy: userEmailAddresses[creatorKey])
            editor.encryptAndSetPassword('password')
        }
        if (!pending) {
            pending = new CatalogueUser(emailAddress: userEmailAddresses.pending,
                                        firstName: 'pending', lastName: 'User',
                                        createdBy: userEmailAddresses[creatorKey],
                                        pending: true,
                                        organisation: 'Oxford', jobTitle: 'tester')
            pending.encryptAndSetPassword('test password')
        }
        if (!author) {
            author = new CatalogueUser(emailAddress: userEmailAddresses.author,
                                       firstName: 'author', lastName: 'User',
                                       createdBy: userEmailAddresses[creatorKey],
                                       tempPassword: SecurityUtils.generateRandomPassword())
        }
        if (!reviewer) {
            reviewer = new CatalogueUser(emailAddress: userEmailAddresses.reviewer,
                                         firstName: 'reviewer', lastName: 'User',
                                         createdBy: userEmailAddresses[creatorKey],
                                         tempPassword: SecurityUtils.generateRandomPassword())
        }
        if (!reader) {
            reader = new CatalogueUser(emailAddress: userEmailAddresses.reader,
                                       firstName: 'reader', lastName: 'User',
                                       createdBy: userEmailAddresses[creatorKey],
                                       tempPassword: SecurityUtils.generateRandomPassword())
        }
        if (!authenticated) {
            authenticated = new CatalogueUser(emailAddress: userEmailAddresses.authenticated,
                                              firstName: 'authenticated', lastName: 'User',
                                              createdBy: userEmailAddresses[creatorKey],
                                              tempPassword: SecurityUtils.generateRandomPassword())
        }
    }

    void getOrCreateBasicGroups(String creatorKey, boolean includeAdmin = true) {
        if (includeAdmin) {
            admins = UserGroup.findByName('administrators')
            if (!admins) createAdminGroup(creatorKey).addToGroupMembers(admin)
        }
        editors = UserGroup.findByName('editors')
        readers = UserGroup.findByName('readers')
        if (!editors) {
            editors = new UserGroup(createdBy: userEmailAddresses[creatorKey],
                                    name: 'editors',
                                    undeleteable: false)
                .addToGroupMembers(containerAdmin)
                .addToGroupMembers(editor)
        }
        if (!readers) {
            readers = new UserGroup(createdBy: userEmailAddresses[creatorKey],
                                    name: 'readers',
                                    undeleteable: false)
                .addToGroupMembers(author)
                .addToGroupMembers(reviewer)
                .addToGroupMembers(reader)
        }
    }
}
