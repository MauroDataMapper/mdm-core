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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.web.databinding.DataBindingUtils
import groovy.util.logging.Slf4j
import org.springframework.core.GenericTypeResolver

@Slf4j
abstract class DataBindReferenceDataModelImporterProviderService<T extends ReferenceDataModelFileImporterProviderServiceParameters> extends
    ReferenceDataModelImporterProviderService<T> {

    abstract ReferenceDataModel importReferenceDataModel(User currentUser, byte[] content)

    List<ReferenceDataModel> importDataModels(User currentUser, byte[] content) {
        throw new ApiBadRequestException('FBIP04', "${getName()} cannot import multiple Reference Data Models")
    }

    @Override
    Class<T> getImporterProviderServiceParametersClass() {
        (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), DataBindReferenceDataModelImporterProviderService)
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    List<ReferenceDataModel> importReferenceDataModels(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        List<ReferenceDataModel> imported = importReferenceDataModels(currentUser, params.importFile.fileContents)
        imported.collect {updateImportedModelFromParameters(it, params, true)}
    }

    @Override
    ReferenceDataModel importReferenceDataModel(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        ReferenceDataModel imported = importReferenceDataModel(currentUser, params.importFile.fileContents)
        updateImportedModelFromParameters(imported, params)
    }

    ReferenceDataModel updateImportedModelFromParameters(ReferenceDataModel referenceDataModel, T params, boolean list = false) {
        if (params.finalised != null) referenceDataModel.finalised = params.finalised
        if (!list && params.modelName) referenceDataModel.label = params.modelName
        referenceDataModel
    }

    ReferenceDataModel bindMapToDataModel(User currentUser, Map referenceDataModelMap) {
        if (!referenceDataModelMap) throw new ApiBadRequestException('FBIP03', 'No ReferenceDataModelMap supplied to import')

        log.debug('Setting map dataElements')
        referenceDataModelMap.dataElements = referenceDataModelMap.remove('dataElements')

        ReferenceDataModel referenceDataModel = new ReferenceDataModel()
        log.debug('Binding map to new ReferenceDataModel instance')
        DataBindingUtils.bindObjectToInstance(referenceDataModel, referenceDataModelMap, null, ['id', 'domainType', 'lastUpdated'], null)

        log.debug('Fixing bound ReferenceDataModel')
        referenceDataModelService.checkImportedReferenceDataModelAssociations(currentUser, referenceDataModel, referenceDataModelMap)

        log.info('Import complete')
        referenceDataModel
    }
}