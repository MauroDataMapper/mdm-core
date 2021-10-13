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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer

import uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider.DataBindDataModelImporterProviderServiceSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 04/08/2017
 */
@Integration
@Rollback
@Slf4j
class DataModelXmlImporterServiceSpec extends DataBindDataModelImporterProviderServiceSpec<DataModelXmlImporterService> {

    DataModelXmlImporterService dataModelXmlImporterService

    @Override
    DataModelXmlImporterService getImporterService() {
        dataModelXmlImporterService
    }

    @Override
    String getImportType() {
        'xml'
    }
}
