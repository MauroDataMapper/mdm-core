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
package uk.ac.ox.softeng.maurodatamapper.dataflow.test

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

abstract class BaseDataFlowIntegrationSpec extends BaseIntegrationSpec {

    DataModel sourceModel
    DataModel targetModel
    DataFlow dataFlow
    Authority testAuthority

    @Override
    void preDomainDataSetup() {
        folder = new Folder(label: 'catalogue', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        checkAndSave(folder)
        testAuthority = new Authority(label: 'Test Authority', url: 'http://localhost', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        checkAndSave(testAuthority)
        sourceModel = BootstrapModels.buildAndSaveSourceDataModel(messageSource, folder, testAuthority)
        targetModel = BootstrapModels.buildAndSaveTargetDataModel(messageSource, folder, testAuthority)
        dataFlow = BootstrapModels.buildAndSaveSampleDataFlow(messageSource)
    }
}
