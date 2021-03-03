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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.parameter.DataFlowFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.web.databinding.DataBindingUtils
import groovy.util.logging.Slf4j
import org.springframework.core.GenericTypeResolver

@Slf4j
abstract class DataBindDataFlowImporterProviderService<T extends DataFlowFileImporterProviderServiceParameters> extends
    DataFlowImporterProviderService<T> {

    abstract DataFlow importDataFlow(User currentUser, byte[] content)

    List<DataFlow> importDataFlows(User currentUser, byte[] content) {
        throw new ApiBadRequestException('FBIP04', "${getName()} cannot import multiple DataFlows")
    }

    @Override
    Class<T> getImporterProviderServiceParametersClass() {
        (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), DataBindDataFlowImporterProviderService)
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    List<DataFlow> importDataFlows(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        long start = System.currentTimeMillis()
        List<DataFlow> imported = importDataFlows(currentUser, params.importFile.fileContents)
        List<DataFlow> updated = imported.collect { updateImportedModelFromParameters(it, params, true) }
        log.info('Imported {} models complete in {}', updated.size(), Utils.timeTaken(start))
        updated
    }

    @Override
    DataFlow importDataFlow(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        long start = System.currentTimeMillis()
        DataFlow imported = importDataFlow(currentUser, params.importFile.fileContents)
        DataFlow updated = updateImportedModelFromParameters(imported, params)
        log.info('Import complete in {}', Utils.timeTaken(start))
        updated
    }

    DataFlow updateImportedModelFromParameters(DataFlow dataFlow, T params, boolean list = false) {
        if (!list && params.modelName) dataFlow.label = params.modelName
        dataFlow
    }

    DataFlow bindMapToDataFlow(User currentUser, Map dataFlowMap) {
        if (!dataFlowMap) throw new ApiBadRequestException('FBIP03', 'No DataFlowMap supplied to import')

        DataFlow dataFlow = new DataFlow()
        log.debug('Binding map to new DataFlow instance')
        DataBindingUtils.bindObjectToInstance(dataFlow, dataFlowMap, null, getImportBlacklistedProperties(), null)

        log.debug('Fixing bound DataFlow')
        dataFlowService.checkImportedDataFlowAssociations(currentUser, dataFlow, dataFlowMap)

        log.debug('Binding complete')
        dataFlow
    }
}