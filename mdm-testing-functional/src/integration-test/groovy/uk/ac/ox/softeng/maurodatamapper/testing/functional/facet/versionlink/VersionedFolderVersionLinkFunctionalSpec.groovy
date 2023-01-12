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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.versionlink

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.ModelVersionLinkFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 * Controller: versionLink
 *  |  POST    | /api/${modelDomainType}/${modelId}/versionLinks        | Action: save
 *  |  GET     | /api/${modelDomainType}/${modelId}/versionLinks        | Action: index
 *  |  DELETE  | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: delete
 *  |  PUT     | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: update
 *  |  GET     | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkController
 */
@Integration
@Slf4j
class VersionedFolderVersionLinkFunctionalSpec extends ModelVersionLinkFunctionalSpec {

    @Override
    String getModelDomainType() {
        'versionedFolders'
    }

    @Override
    @Transactional
    String getModelId() {
        VersionedFolder.findByLabel('Functional Test VersionedFolder').id.toString()
    }

    @Transactional
    @Override
    String getTargetModelId() {
        VersionedFolder.findByLabel('Functional Test VersionedFolder 2').id.toString()
    }

    @Override
    String getTargetModelDomainType() {
        'VersionedFolder'
    }

    @Override
    String getModelJsonString() {
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