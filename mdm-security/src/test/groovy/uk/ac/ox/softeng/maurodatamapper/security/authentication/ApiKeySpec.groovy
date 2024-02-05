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
package uk.ac.ox.softeng.maurodatamapper.security.authentication

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

import java.time.LocalDate

@Slf4j
class ApiKeySpec extends MdmDomainSpec<ApiKey> implements DomainUnitTest<ApiKey>, SecurityUsers {

    LocalDate expiry

    def setup() {
        log.debug('Setting up ApiKeySpec')
        mockDomains(Edit, CatalogueUser)
        implementSecurityUsers('unitTest')
        expiry = LocalDate.now()
    }

    @Override
    void setValidDomainOtherValues() {
        domain.expiryDate = expiry
        domain.name = 'Global Access'
        domain.catalogueUser = editor
    }

    @Override
    void verifyDomainOtherConstraints(ApiKey domain) {
        domain.name == 'Global Access'
        domain.expiryDate == expiry
        !domain.refreshable
        !domain.expired
    }


    void 'test expiry'() {
        given:
        setValidDomainValues()
        domain.expiryDate = expiry.minusDays(1)

        expect:
        domain.isExpired()
    }
}
