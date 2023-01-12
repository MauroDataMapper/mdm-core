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
package uk.ac.ox.softeng.maurodatamapper.core.facet.versionlink

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.ModelVersionLinkFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkController
 */
@Integration
@Slf4j
class VersionedFolderVersionLinkFunctionalSpec extends ModelVersionLinkFunctionalSpec {

    @Shared
    VersionedFolder versionedFolder
    @Shared
    VersionedFolder otherVersionedFolder

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        versionedFolder = new VersionedFolder(label: 'Functional Test VersionedFolder', authority: testAuthority,
                                              createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true)
        otherVersionedFolder = new VersionedFolder(label: 'Functional Test VersionedFolder 2', authority: testAuthority,
                                                   createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec VersionedFolderVersionLinkFunctionalSpec')
        cleanUpResources(Folder,)
    }

    @Override
    UUID getCatalogueItemId() {
        versionedFolder.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'versionedFolders'
    }

    @Override
    String getTargetModelId() {
        otherVersionedFolder.id.toString()
    }

    @Override
    String getTargetModelDomainType() {
        'VersionedFolder'
    }

    @Override
    String getModelDomainType() {
        'VersionedFolder'
    }

    @Override
    String getSourceModelJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test VersionedFolder"
  }'''
    }

    @Override
    String getTargetModelJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test VersionedFolder 2"
  }'''
    }
}