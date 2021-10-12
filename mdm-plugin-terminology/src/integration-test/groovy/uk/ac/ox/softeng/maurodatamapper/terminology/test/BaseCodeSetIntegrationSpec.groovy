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
package uk.ac.ox.softeng.maurodatamapper.terminology.test

import uk.ac.ox.softeng.maurodatamapper.core.admin.AdminService
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import groovy.util.logging.Slf4j

/**
 * @since 05/10/2020
 */
@Slf4j
abstract class BaseCodeSetIntegrationSpec extends BaseIntegrationSpec {

    Authority testAuthority
    AdminService adminService
    CodeSetService codeSetService
    TerminologyService terminologyService
    CodeSet simpleCodeSet
    CodeSet complexCodeSet
    Terminology simpleTerminology

    Folder getTestFolder() {
        folder
    }

    @Override
    void preDomainDataSetup() {
        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
        testAuthority = Authority.findByLabel('Test Authority')
        checkAndSave(testAuthority)
        simpleCodeSet = BootstrapModels.buildAndSaveSimpleCodeSet(messageSource, folder, testAuthority)
        complexCodeSet = BootstrapModels.buildAndSaveComplexCodeSet(messageSource, folder, terminologyService, testAuthority)
        simpleTerminology = Terminology.findByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME)
    }

    static String replaceWithTestAuthority(String exported) {
        exported.replace(/Mauro Data Mapper/, 'Test Authority')
    }
}
