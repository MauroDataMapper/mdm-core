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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlowService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 07/01/2021
 */
@Slf4j
@CompileStatic
abstract class DataFlowExporterProviderService extends ExporterProviderService {

    @Autowired
    DataFlowService dataFlowService

    abstract ByteArrayOutputStream exportDataFlow(User currentUser, DataFlow dataFlow) throws ApiException

    abstract ByteArrayOutputStream exportDataFlows(User currentUser, List<DataFlow> dataFlows) throws ApiException

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException {
        DataFlow dataFlow = dataFlowService.get(domainId)
        if (!dataFlow) {
            log.error('Cannot find model id [{}] to export', domainId)
            throw new ApiInternalException('DFEP01', "Cannot find model id [${domainId}] to export")
        }
        exportDataFlow(currentUser, dataFlow)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds) throws ApiException {
        List<DataFlow> dataFlows = []
        List<UUID> cannotExport = []
        domainIds.each {
            DataFlow dataFlow = dataFlowService.get(it)
            if (!dataFlow) {
                cannotExport.add it
            } else dataFlows.add dataFlow
        }
        log.warn('Cannot find model ids [{}] to export', cannotExport)
        exportDataFlows(currentUser, dataFlows)
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    String getProviderType() {
        "DataFlow${ProviderType.EXPORTER.name}"
    }
}
