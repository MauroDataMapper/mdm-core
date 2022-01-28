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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

@Slf4j
class CatalogueUserSpec extends MdmDomainSpec<CatalogueUser> implements DomainUnitTest<CatalogueUser> {

    @Override
    void setValidDomainOtherValues() {
        domain.emailAddress = 'test@admin.com'
        domain.firstName = 'test'
        domain.lastName = 'user'
    }

    @Override
    void verifyDomainOtherConstraints(CatalogueUser domain) {
        domain.emailAddress == 'test@admin.com'
        domain.firstName == 'test'
        domain.lastName == 'user'
    }

    void "test saving user with nothing set"() {

        when:
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        CatalogueUser.count() == 0

        and:
        domain.hasErrors()
        domain.errors.allErrors.size() == 4

        and:
        domain.salt.length == 8
    }

    void "test saving user with bare bones set"() {
        given:
        domain.emailAddress = 'test@admin.com'
        domain.firstName = 'test'
        domain.lastName = 'user'
        domain.createdBy = 'unit-test@test.com'

        expect:
        checkAndSave(domain)
        CatalogueUser.count() == 1

        and:
        domain.salt.length == 8
    }

    void 'test password setting'() {
        given:
        domain.emailAddress = 'test@admin.com'

        when:
        domain.encryptAndSetPassword 'test'

        then:
        domain.password != 'test'.bytes
    }
}

