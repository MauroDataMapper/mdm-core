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
abstract class CatalogueItemFacetFunctionalSpec<D extends GormEntity> extends ResourceFunctionalSpec<D> {

    @Shared
    Folder folder

    @Shared
    Authority testAuthority

    abstract UUID getCatalogueItemId()

    abstract String getCatalogueItemDomainResourcePath()

    abstract String getFacetResourcePath()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainResourcePath()}/${getCatalogueItemId()}/${getFacetResourcePath()}"
    }

    @OnceBefore
    @Transactional
    def checkAndSetupFolderAndAuthority() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec CatalogueItemFacetFunctionalSpec')
        cleanUpResources(Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
    }
}