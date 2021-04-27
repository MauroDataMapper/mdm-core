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
package uk.ac.ox.softeng.maurodatamapper.test.functional.facet


import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

@Slf4j
abstract class ContainerFacetFunctionalSpec<D extends GormEntity> extends ResourceFunctionalSpec<D> {

    @Shared
    Authority testAuthority

    abstract UUID getContainerId()

    abstract String getContainerDomainResourcePath()

    abstract String getFacetResourcePath()

    @Override
    String getResourcePath() {
        "${getContainerDomainResourcePath()}/${getContainerId()}/${getFacetResourcePath()}"
    }

    @OnceBefore
    @Transactional
    def checkAndSetupFolderAndAuthority() {
        log.debug('Check and setup test data')
        testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec ContainerFacetFunctionalSpec')
        cleanUpResources(Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
    }
}