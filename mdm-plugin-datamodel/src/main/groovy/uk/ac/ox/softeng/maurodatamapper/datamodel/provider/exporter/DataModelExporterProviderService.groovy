/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 07/03/2018
 */
@Slf4j
@CompileStatic
abstract class DataModelExporterProviderService extends ExporterProviderService {

    @Autowired
    DataModelService dataModelService

    abstract ByteArrayOutputStream exportDataModel(User currentUser, DataModel dataModel, Map<String, Object> parameters) throws ApiException

    abstract ByteArrayOutputStream exportDataModels(User currentUser, List<DataModel> dataModels, Map<String, Object> parameters) throws ApiException

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId, Map<String, Object> parameters) throws ApiException {
        DataModel dataModel = dataModelService.get(domainId)
        if (!dataModel) {
            log.error('Cannot find model id [{}] to export', domainId)
            throw new ApiInternalException('DMEP01', "Cannot find model id [${domainId}] to export")
        }
        exportDataModel(currentUser, dataModel, parameters)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds, Map<String, Object> parameters) throws ApiException {
        List<DataModel> dataModels = []
        List<UUID> cannotExport = []
        domainIds?.unique()?.each {
            DataModel dataModel = dataModelService.get(it)
            if (dataModel) dataModels << dataModel
            else cannotExport << it
        }
        if (!dataModels) throw new ApiBadRequestException('DMEP01', "Cannot find DataModel IDs [${cannotExport}] to export")
        if (cannotExport) log.warn('Cannot find DataModel IDs [{}] to export', cannotExport)
        exportDataModels(currentUser, dataModels, parameters)
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    String getProviderType() {
        "DataModel${ProviderType.EXPORTER.name}"
    }

    @Override
    String getFileName(MdmDomain domain) {
        "${(domain as Model).label}.${getFileExtension()}"
    }
}
