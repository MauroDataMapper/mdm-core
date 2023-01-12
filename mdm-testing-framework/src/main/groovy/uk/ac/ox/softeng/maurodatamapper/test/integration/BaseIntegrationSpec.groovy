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
package uk.ac.ox.softeng.maurodatamapper.test.integration

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.HibernateSearchIndexingService
import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.IdSecuredUserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@Slf4j
abstract class BaseIntegrationSpec extends MdmSpecification {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    MessageSource messageSource

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    HibernateSearchIndexingService hibernateSearchIndexingService

    UUID id

    Folder folder

    abstract void setupDomainData()

    void setupData() {
        long start = System.currentTimeMillis()

        preDomainDataSetup()

        setupDomainData()

        postDomainDataSetup()

        // This log marker allows us to ignore all the inserts and DB queries in the logs prior,
        // thus allowing analysis of the SQL actioned for each test
        log.debug('==> Test data setup and inserted into database took {} <==', Utils.timeTaken(start))
    }

    void preDomainDataSetup() {
        // No-op allows any extending classes to perform actions
    }

    void postDomainDataSetup() {
        // No-op allows any extending classes to perform actions
    }

    IdSecuredUserSecurityPolicyManager getAdminSecurityPolicyManager() {
        new IdSecuredUserSecurityPolicyManager(admin, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
    }

    IdSecuredUserSecurityPolicyManager getEditorSecurityPolicyManager() {
        new IdSecuredUserSecurityPolicyManager(editor, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
    }
}
