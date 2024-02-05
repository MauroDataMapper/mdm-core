/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.test

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.TestUser
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.validation.Validateable
import grails.validation.ValidationException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.spockframework.util.Assert
import org.springframework.context.MessageSource
import spock.lang.Specification

/**
 * @since 08/10/2019
 */
@Slf4j
@CompileStatic
abstract class MdmSpecification extends Specification {

    abstract MessageSource getMessageSource()

    static UUID adminId
    static UUID editorId
    static UUID reader1Id
    static UUID reader2Id
    static UUID pendingId

    long startTime

    def setupSpec() {
        log.debug('Setup User Ids')
        adminId = UUID.randomUUID()
        editorId = UUID.randomUUID()
        reader1Id = UUID.randomUUID()
        reader2Id = UUID.randomUUID()
        pendingId = UUID.randomUUID()
    }

    def setup() {
        startTime = System.currentTimeMillis()
        log.warn('--- {} --- {} ---', specificationContext.currentSpec.displayName, specificationContext.currentIteration.displayName)
    }

    def cleanup() {
        log.warn('--- {} --- {} >>> {} ---', specificationContext.currentSpec.displayName, specificationContext.currentIteration.displayName,
                 Utils.timeTaken(startTime))
    }

    void check(GormEntity domainObj) {
        try {
            GormUtils.check(messageSource, domainObj)
        } catch (ValidationException ex) {
            Assert.fail(ex.message)
        }
    }

    void check(Validateable domainObj) {
        try {
            GormUtils.check(messageSource, domainObj)
        } catch (ValidationException ex) {
            Assert.fail(ex.message)
        }
    }

    void checkAndSave(GormEntity domainObj) {
        try {
            GormUtils.checkAndSave(messageSource, domainObj)
        } catch (ValidationException ex) {
            Assert.fail(ex.message)
        }
    }

    void checkAndSave(GormEntity... domainObjs) {
        try {
            GormUtils.checkAndSave(messageSource, domainObjs)
        } catch (ValidationException ex) {
            Assert.fail(ex.message)
        }
    }

    User getAdmin() {
        new TestUser(emailAddress: StandardEmailAddress.ADMIN,
                     firstName: 'Admin',
                     lastName: 'User',
                     organisation: 'Oxford BRC Informatics',
                     jobTitle: 'God',
                     id: adminId)
    }

    User getEditor() {
        new TestUser(emailAddress: 'editor@test.com',
                     firstName: 'editor', lastName: 'User', id: editorId)
    }

    User getPending() {
        new TestUser(emailAddress: 'pending@test.com',
                     firstName: 'pending', lastName: 'User', id: pendingId)
    }

    User getReader1() {
        new TestUser(emailAddress: 'reader1@test.com',
                     firstName: 'reader1', lastName: 'User', id: reader1Id)
    }

    User getReader2() {
        new TestUser(emailAddress: 'reader2@test.com',
                     firstName: 'reader2', lastName: 'User', id: reader2Id)
    }
}
