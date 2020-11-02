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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@CompileStatic
abstract class ReferenceDataModelExporterProviderService extends ExporterProviderService {

    @Autowired
    ReferenceDataModelService referenceDataModelService

    abstract ByteArrayOutputStream exportReferenceDataModel(User currentUser, ReferenceDataModel referenceDataModel) throws ApiException

    abstract ByteArrayOutputStream exportReferenceDataModels(User currentUser, List<ReferenceDataModel> referenceDataModels) throws ApiException

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException {
        ReferenceDataModel referenceDataModel = referenceDataModelService.get(domainId)
        if (!referenceDataModel) {
            log.error('Cannot find model id [{}] to export', domainId)
            throw new ApiInternalException('RDMEP01', "Cannot find model id [${domainId}] to export")
        }
        exportReferenceDataModel(currentUser, referenceDataModel)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds) throws ApiException {
        List<ReferenceDataModel> referenceDataModels = []
        List<UUID> cannotExport = []
        domainIds.each {
            ReferenceDataModel referenceDataModel = referenceDataModelService.get(it)
            if (!referenceDataModel) {
                cannotExport.add it
            } else referenceDataModels.add referenceDataModel
        }
        log.warn('Cannot find model ids [{}] to export', cannotExport)
        exportReferenceDataModels(currentUser, referenceDataModels)
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    String getProviderType() {
        "ReferenceDataModel${ProviderType.EXPORTER.name}"
    }
}
