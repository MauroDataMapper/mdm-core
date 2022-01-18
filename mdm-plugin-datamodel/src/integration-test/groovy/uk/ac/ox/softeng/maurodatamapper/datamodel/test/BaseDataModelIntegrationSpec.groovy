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
package uk.ac.ox.softeng.maurodatamapper.datamodel.test

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import groovy.util.logging.Slf4j

/**
 * @since 03/03/2020
 */
@Slf4j
abstract class BaseDataModelIntegrationSpec extends BaseIntegrationSpec {

    Authority testAuthority
    DataModel dataModel

    Folder getTestFolder() {
        folder
    }

    @Override
    void preDomainDataSetup() {
        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
        testAuthority = Authority.findByLabel('Test Authority')
        checkAndSave(testAuthority)
    }

    DataModel buildSimpleDataModel() {
        BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder, testAuthority)
    }

    DataModel buildSimpleDataModel(Folder folder) {
        BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder, testAuthority)
    }

    DataModel buildComplexDataModel() {
        BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder, testAuthority)
    }

    DataModel buildComplexDataModel(Folder folder) {
        BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder, testAuthority)
    }
}
