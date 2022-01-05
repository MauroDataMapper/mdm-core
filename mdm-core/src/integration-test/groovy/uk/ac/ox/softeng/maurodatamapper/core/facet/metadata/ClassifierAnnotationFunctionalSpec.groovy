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
package uk.ac.ox.softeng.maurodatamapper.core.facet.metadata

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.ContainerMetadataFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * Where facet owner is a DataModel
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController
 */
@Integration
@Slf4j
class ClassifierMetadataFunctionalSpec extends ContainerMetadataFunctionalSpec {

    @Shared
    Classifier classifier

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        classifier = new Classifier(label: 'Functional Test Classifier', createdBy: 'functionalTest@test.com').save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(Classifier)
    }

    @Override
    UUID getContainerId() {
        classifier.id
    }

    @Override
    String getContainerDomainResourcePath() {
        'classifiers'
    }


}