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
package uk.ac.ox.softeng.maurodatamapper.core.provider.importer

import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class ModelImporterProviderService<M extends Model, P extends ModelImporterProviderServiceParameters>
    extends ImporterProviderService<M, P> {

    @Autowired
    ClassifierService classifierService

    abstract ModelService getModelService()

    abstract M importModel(User currentUser, P params)

    abstract List<M> importModels(User currentUser, P params)

    @Override
    M importDomain(User currentUser, ModelImporterProviderServiceParameters params) {
        M model = importModel(currentUser, params as P)
        if (!model) return null
        M updated = updateImportedModelFromParameters(model, params as P, false)
        checkImport(currentUser, updated, params as P)
    }

    @Override
    List<M> importDomains(User currentUser, ModelImporterProviderServiceParameters params) {
        List<M> models = importModels(currentUser, params as P)
        models?.collect {
            M updated = updateImportedModelFromParameters(it, params as P, true)
            checkImport(currentUser, updated, params as P)
        }
        models
    }

    M checkImport(User currentUser, M importedModel, P params) {
        classifierService.checkClassifiers(currentUser, importedModel)

        // Mst check this first to ensure its in place for finding existing models
        modelService.checkAuthority(currentUser, importedModel, params.useDefaultAuthority)

        //If a model exists with the same Id as the import, finalise the old one
        modelService.checkDocumentationVersion(importedModel, params.importAsNewDocumentationVersion, currentUser)

        //if branchModel option is checked, find and finalise latest non default version
        //else if no version is found or version is the default, create a finalised version of default instead
        modelService.checkBranchModelVersion(importedModel, params.importAsNewBranchModelVersion, params.newBranchName, currentUser)

        // Need to do all the branch management before we finalise the model otherwise the branch work overrides everything
        modelService.checkFinaliseModel(importedModel, params.finalised, params.importAsNewBranchModelVersion)

        modelService.propagateFromPreviousVersion(importedModel, params.propagateFromPreviousVersion, currentUser)

        importedModel
    }

    M updateImportedModelFromParameters(M importedModel, P params, boolean list = false) {
        if (params.finalised != null) importedModel.finalised = params.finalised
        if (!list && params.modelName) importedModel.label = params.modelName
        if (params.author) importedModel.author = params.author
        if (params.organisation) importedModel.organisation = params.organisation
        if (params.description) importedModel.description = params.description
        importedModel
    }
}
