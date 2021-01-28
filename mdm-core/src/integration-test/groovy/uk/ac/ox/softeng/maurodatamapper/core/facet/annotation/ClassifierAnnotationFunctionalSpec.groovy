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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.annotation

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Shared
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.ContainerAnnotationFunctionalSpec

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController
 */
@Integration
@Slf4j
class ClassifierAnnotationFunctional extends ContainerAnnotationFunctionalSpec {

    @Shared
    Classifier classifier


    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        classifier = new Classifier(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST).save(flush: true)
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PluginContainerFunctionalSpec')
        cleanUpResources(Classifier)
    }

    @Override
    UUID getContainerId() {
        classifier.id
    }

    @Override
    String getContainerDomainResourcePath() {
        'classifier'
    }
}