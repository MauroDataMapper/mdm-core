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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer

import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 07/03/2018
 */
@CompileStatic
abstract class DataModelImporterProviderService<T extends DataModelImporterProviderServiceParameters>
    extends ImporterProviderService<DataModel, T> {

    @Autowired
    DataModelService dataModelService

    @Autowired
    ClassifierService classifierService

    abstract DataModel importDataModel(User currentUser, T params)

    abstract List<DataModel> importDataModels(User currentUser, T params)

    @CompileDynamic
    @Override
    DataModel importDomain(User currentUser, T params) {
        DataModel dataModel = importDataModel(currentUser, params)
        if (!dataModel) return null
        if (params.modelName) dataModel.label = params.modelName
        checkImport(currentUser, dataModel, params)
    }

    @CompileDynamic
    @Override
    List<DataModel> importDomains(User currentUser, T params) {
        List<DataModel> dataModels = importDataModels(currentUser, params)
        dataModels?.collect {
            checkImport(currentUser, it, params)
        }
    }

    @Override
    String getProviderType() {
        "DataModel${ProviderType.IMPORTER.name}"
    }

    DataModel checkImport(User currentUser, DataModel dataModel, T params) {
        dataModelService.checkfinaliseModel(dataModel, params.finalised)
        dataModelService.checkDocumentationVersion(dataModel, params.importAsNewDocumentationVersion, currentUser)
        dataModelService.checkBranchModelVersion(dataModel, params.importAsNewBranchModelVersion, params.newBranchName, currentUser)
        classifierService.checkClassifiers(currentUser, dataModel)

        dataModel.dataClasses?.each { dc ->
            classifierService.checkClassifiers(currentUser, dc)
            dc.dataElements?.each { de ->
                classifierService.checkClassifiers(currentUser, de)
            }
        }
        dataModel.dataTypes?.each { dt ->
            classifierService.checkClassifiers(currentUser, dt)
        }
        dataModel
    }

    DataModel updateImportedModelFromParameters(DataModel dataModel, T params, boolean list = false) {
        if (params.finalised != null) dataModel.finalised = params.finalised
        if (!list && params.modelName) dataModel.label = params.modelName
        if (params.author) dataModel.author = params.author
        if (params.organisation) dataModel.organisation = params.organisation
        if (params.description) dataModel.description = params.description
        dataModel
    }
}
