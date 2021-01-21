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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.web.databinding.DataBindingUtils
import groovy.util.logging.Slf4j
import org.springframework.core.GenericTypeResolver

@Slf4j
abstract class DataBindDataModelImporterProviderService<T extends DataModelFileImporterProviderServiceParameters> extends
    DataModelImporterProviderService<T> {

    abstract DataModel importDataModel(User currentUser, byte[] content)

    List<DataModel> importDataModels(User currentUser, byte[] content) {
        throw new ApiBadRequestException('FBIP04', "${getName()} cannot import multiple DataModels")
    }

    @Override
    Class<T> getImporterProviderServiceParametersClass() {
        (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), DataBindDataModelImporterProviderService)
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    List<DataModel> importDataModels(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        long start = System.currentTimeMillis()
        List<DataModel> imported = importDataModels(currentUser, params.importFile.fileContents)
        List<DataModel> updated = imported.collect { updateImportedModelFromParameters(it, params, true) }
        log.info('Imported {} models complete in {}', updated.size(), Utils.timeTaken(start))
        updated
    }

    @Override
    DataModel importDataModel(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        long start = System.currentTimeMillis()
        DataModel imported = importDataModel(currentUser, params.importFile.fileContents)
        DataModel updated = updateImportedModelFromParameters(imported, params)
        log.info('Import complete in {}', Utils.timeTaken(start))
        updated
    }

    DataModel bindMapToDataModel(User currentUser, Map dataModelMap) {
        if (!dataModelMap) throw new ApiBadRequestException('FBIP03', 'No DataModelMap supplied to import')

        log.debug('Setting map dataClasses')
        dataModelMap.dataClasses = dataModelMap.remove('childDataClasses')

        DataModel dataModel = new DataModel()
        log.debug('Binding map to new DataModel instance')
        DataBindingUtils.bindObjectToInstance(dataModel, dataModelMap, null, getImportBlacklistedProperties(), null)

        log.debug('Fixing bound DataModel')
        dataModelService.checkImportedDataModelAssociations(currentUser, dataModel, dataModelMap)

        log.debug('Binding complete')
        dataModel
    }
}