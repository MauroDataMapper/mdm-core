/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.exporter.ExporterService
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter.DataFlowExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.DataFlowImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.parameter.DataFlowImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService

import grails.gorm.transactions.Transactional
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
@SuppressWarnings('GroovyAssignabilityCheck')
class DataFlowController extends EditLoggingController<DataFlow> {
    static responseFormats = ['json', 'xml']

    DataFlowService dataFlowService
    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    ImporterService importerService
    ExporterService exporterService

    DataModelService dataModelService

    @Autowired(required = false)
    Set<DataFlowExporterProviderService> exporterProviderServices

    @Autowired(required = false)
    Set<DataFlowImporterProviderService> importerProviderServices

    DataFlowController() {
        super(DataFlow)
    }

    def exporterProviders() {
        log.debug('exporterProviders')
        respond exporterProviders: getExporterProviderServices()
    }

    def importerProviders() {
        log.debug('importerProviders')
        respond importerProviders: getImporterProviderServices()
    }

    def updateDiagramLayout() {
        params.noHistory = true
        params.id = params.dataFlowId
        update()
    }

    def exportDataFlow() {
        DataFlowExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(
            params.exporterNamespace, params.exporterName, params.exporterVersion
        )

        if (!exporter) {
            return notFound(DataFlowExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}")
        }

        DataFlow instance = queryForResource params.dataFlowId

        if (!instance) return notFound(params.dataFlowId)

        // Extract body to map and add the params from the url
        Map exporterParameters = extractRequestBodyToMap()
        exporterParameters.putAll(params)

        log.info("Exporting DataFlow using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomain(currentUser, exporter, params.dataFlowId as String, params)
        log.info('Export complete')
        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'DataFlow could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: "${instance.label}.${exporter.fileExtension}", contentType: exporter.contentType)
    }

    def exportDataFlows() {
        DataFlowExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(params.exporterNamespace, params.exporterName,
                                                                                                              params.exporterVersion)
        if (!exporter) {
            return notFound(DataFlowExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}")
        }

        // Extract body to map and add the params from the url
        Map exporterParameters = extractRequestBodyToMap()
        exporterParameters.putAll(params)

        if (!exporterParameters.dataFlowIds) throw new ApiBadRequestException('DMIXX', 'DataFlowIds must be supplied in the request body')

        if (!exporter.canExportMultipleDomains()) {
            params.dataFlowId = exporterParameters.dataFlowIds.first()
            return exportDataFlow()
        }

        log.info("Exporting DataFlows using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomains(currentUser, exporter, exporterParameters.dataFlowIds, exporterParameters)
        log.info('Export complete')

        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'DataFlows could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: "DataFlows.${exporter.fileExtension}", contentType: exporter.contentType)
    }

    private DataFlowImporterProviderService findImporter() {
        mauroDataMapperServiceProviderService.findImporterProvider(
            params.importerNamespace, params.importerName, params.importerVersion
        ) as DataFlowImporterProviderService
    }

    private onImporterNotFound() {
        notFound(DataFlowImporterProviderService, "${params.importerNamespace}:${params.importerName}:${params.importerVersion}")
    }

    @Transactional
    def importDataFlow() throws ApiException {
        DataFlowImporterProviderService importer = findImporter()

        if (!importer) {
            onImporterNotFound()
            return
        }

        DataFlowImporterProviderServiceParameters importerProviderServiceParameters

        if (request.contentType.startsWith(MimeType.MULTIPART_FORM.name)) {
            importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(
                importer, request as AbstractMultipartHttpServletRequest)
        } else {
            importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(importer, request)
        }

        def errors = importerService.validateParameters(importerProviderServiceParameters as DataFlowImporterProviderServiceParameters,
                                                        importer.importerProviderServiceParametersClass)

        if (errors.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond errors
            return
        }

        DataFlow dataFlow = importer.importDomain(currentUser, importerProviderServiceParameters)

        if (!dataFlow) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No dataflow imported')
        }

        dataFlow.validate()

        if (dataFlow.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond dataFlow.errors
            return
        }

        log.debug('No errors in imported dataflow')

        DataFlow savedDataFlow = dataFlowService.save(dataFlow)

        log.info('Single DataFlow Import complete')

        if (params.boolean('returnList')) {
            respond([savedDataFlow], status: CREATED, view: 'index')
        } else {
            respond savedDataFlow, status: CREATED, view: 'show'
        }
    }

    @Transactional
    def importDataFlows() throws ApiException {
        DataFlowImporterProviderService importer = findImporter()

        if (!importer) {
            onImporterNotFound()
            return
        }

        // Default through to importing single model
        // This may result in errors due to file containing multiple models, but that should be handled
        if (!importer.canImportMultipleDomains()) {
            params.returnList = true
            return importDataFlow()
        }

        DataFlowImporterProviderServiceParameters importerProviderServiceParameters

        if (request.contentType.startsWith(MimeType.MULTIPART_FORM.name)) {
            importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(
                importer, request as AbstractMultipartHttpServletRequest)
        } else {
            importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(importer, request)
        }

        def errors = importerService.validateParameters(importerProviderServiceParameters, importer.importerProviderServiceParametersClass)

        if (errors.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond errors
            return
        }

        List<DataFlow> result = importerService.importDomains(currentUser, importer, importerProviderServiceParameters)

        if (!result) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No data flow imported')
        }

        result.each { df ->
            df.validate()
        }

        if (result.any { it.hasErrors() }) {
            log.debug('Errors found in imported dataflows')
            transactionStatus.setRollbackOnly()
            respond(getMultiErrorResponseMap(result), view: '/error', status: UNPROCESSABLE_ENTITY)
            return
        }

        log.debug('No errors in imported dataflows')
        List<DataFlow> savedDataFlows = result.collect {
            DataFlow saved = dataFlowService.save(it)
            saved
        }
        log.debug('Saved all dataflows')
        log.info('Multi-Dataflow Import complete')

        respond savedDataFlows, status: CREATED, view: 'index'

    }

    protected DataFlow queryForResource(Serializable id) {
        dataFlowService.findByTargetDataModelIdAndId(params.dataModelId, id)
    }

    @Override
    protected List<DataFlow> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'label'
        if (params.type) {
            if (params.type == 'source') {
                return dataFlowService.findAllReadableBySourceDataModel(currentUserSecurityPolicyManager,
                                                                        params.dataModelId,
                                                                        params)
            } else {
                dataFlowService.findAllReadableByTargetDataModel(currentUserSecurityPolicyManager, params.dataModelId, params)
            }

        }
        dataFlowService.findAllReadableChainedByDataModel(currentUserSecurityPolicyManager, params.dataModelId)
    }

    @Override
    void serviceDeleteResource(DataFlow resource) {
        dataFlowService.delete(resource, true)
    }

    @Override
    protected DataFlow createResource() {
        DataFlow resource = super.createResource() as DataFlow
        resource.target = dataModelService.get(params.dataModelId)
        dataModelService.setModelIsFromModel(resource.target, resource.source, currentUser)
        resource
    }
}
