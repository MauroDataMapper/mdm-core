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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelImportFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Integration
@Slf4j
class DataClassModelImportFunctionalSpec extends ModelImportFunctionalSpec {

    @Override
    String getIndexPath() {
        "dataModels/${getCatalogueItemId()}/dataClasses"
    }

    @Override
    String getModelImportPath() {
        "dataModels/${getCatalogueItemId()}/modelImports"
    }

    @Transactional
    @Override
    String getImportedCatalogueItemId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id,
                                        BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL).get().id.toString()
    }

    @Override
    String getImportedCatalogueItemDomainType() {
        DataClass.simpleName
    }

    @Transactional
    @Override
    String getCatalogueItemId() {
        DataModel.findByLabel(BootstrapModels.IMPORTING_DATAMODEL_NAME_2).id.toString()
    }

    @Override
    String getCatalogueItemDomainType() {
        DataModel.simpleName
    }

    @Override
    void verifyIndex() {
        assert responseBody().count == 1
        assert responseBody().items.size() == 1
    }

}
