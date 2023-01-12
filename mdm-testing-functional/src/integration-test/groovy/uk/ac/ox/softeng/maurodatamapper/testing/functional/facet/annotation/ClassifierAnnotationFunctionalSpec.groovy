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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.annotation

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.ContainerAnnotationFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 *  Controller: annotation
 *  |  POST    | /api/${containerDomainType}/${containerId}/annotations        | Action: save
 *  |  GET     | /api/${containerDomainType}/${containerId}/annotations        | Action: index
 *  |  DELETE  | /api/${containerDomainType}/${containerId}/annotations/${id}  | Action: delete
 *  |  GET     | /api/${containerDomainType}/${containerId}/annotations/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController
 */
@Integration
@Slf4j
class ClassifierAnnotationFunctionalSpec extends ContainerAnnotationFunctionalSpec {

    @Transactional
    @Override
    String getContainerId() {
        Classifier.findByLabel('Functional Test Classifier').id.toString()
    }

    @Override
    String getContainerDomainType() {
        'classifiers'
    }

    @Override
    String getEditorIndexJson() {
        '{"count": 0,"items": []}'
    }
}