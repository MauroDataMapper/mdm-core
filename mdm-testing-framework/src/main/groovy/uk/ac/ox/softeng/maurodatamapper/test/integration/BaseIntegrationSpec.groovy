/*
 * Copyright 2020 University of Oxford
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
import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.LuceneIndexingService
import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.IdSecuredUserSecurityPolicyManager

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
    LuceneIndexingService luceneIndexingService

    UUID id

    Folder folder

    abstract void setupDomainData()

    void setupData() {

        // Remove any indexes which currently exist from previous tests
        luceneIndexingService.purgeAllIndexes()

        preDomainDataSetup()

        setupDomainData()

        postDomainDataSetup()

        // Flush all the new data indexes to the files
        luceneIndexingService.flushIndexes()

        // This log marker allows us to ignore all the inserts and DB queries in the logs prior,
        // thus allowing analysis of the SQL actioned for each test
        log.debug("==> Test data setup and inserted into database <==")
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
