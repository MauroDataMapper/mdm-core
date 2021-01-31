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
package uk.ac.ox.softeng.maurodatamapper.dataflow


import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.exporter.ExporterService
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService

import groovy.util.logging.Slf4j
import org.grails.web.json.JSONArray

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

@SuppressWarnings('GroovyAssignabilityCheck')
@Slf4j
class DataFlowController extends EditLoggingController<DataFlow> {
    static responseFormats = ['json', 'xml']

    DataFlowService dataFlowService
    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    ImporterService importerService
    ExporterService exporterService

    DataModelService dataModelService

    //    @Autowired(required = false)
    //    Set<DataFlowExporterProviderService> exporterProviderServices
    //
    //    @Autowired(required = false)
    //    Set<DataFlowImporterProviderService> importerProviderServices

    DataFlowController() {
        super(DataFlow)
    }

    Set<ExporterProviderService> getExporterProviderServices() {
        [] as Set
    }

    Set<ImporterProviderService> getImporterProviderServices() {
        [] as Set
    }

    def exporterProviders() {
        respond exporterProviders: getExporterProviderServices()
    }

    def importerProviders() {
        respond importerProviders: getImporterProviderServices()
    }

    def updateDiagramLayout() {
        params.noHistory = true
        params.id = params.dataFlowId
        update()
    }

    def exportDataFlow() {
        ExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(
            params.exporterNamespace, params.exporterName, params.exporterVersion
        )

        if (!exporter) {
            return notFound(ExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}")
        }

        DataFlow instance = queryForResource params.dataFlowId

        if (!instance) return notFound(params.dataFlowId)
        log.info("Exporting DataFlow using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomain(currentUser, exporter, params.dataFlowId as String)
        log.info('Export complete')
        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'DataFlow could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: "${instance.label}.${exporter.fileExtension}", contentType: exporter.fileType)
    }

    def exportDataFlows() {
        ExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(params.exporterNamespace, params.exporterName,
                                                                                                      params.exporterVersion)
        if (!exporter) {
            return notFound(ExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}")
        }

        def json = request.getJSON()
        List<String> dataFlowIds = []
        if (json && json instanceof JSONArray) {
            dataFlowIds = json
        }

        if (!exporter.canExportMultipleDomains()) {
            params.dataFlowId = dataFlowIds.first()
            return exportDataFlow()
        }

        log.info("Exporting DataFlows using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomains(currentUser, exporter, dataFlowIds)
        log.info('Export complete')

        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'DataFlows could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: "DataFlows.${exporter.fileExtension}", contentType: exporter.fileType)
    }

    /*
        @Transactional
        def importDataFlow() throws ApiException {
            ImporterProviderService importer = mauroDataMapperServiceProviderService.findImporterProvider(
                params.importerNamespace, params.importerName, params.importerVersion
            ) as ImporterProviderService
            if (!importer) {
                notFound(ImporterProviderService, "${params.importerNamespace}:${params.importerName}:${params.importerVersion}"
                )
                return
            }

            ModelImporterProviderServiceParameters importerProviderServiceParameters

            if (request.contentType.startsWith(MimeType.MULTIPART_FORM.name)) {
                importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(
                    importer, request as AbstractMultipartHttpServletRequest)
            } else {
                importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(importer, request)
            }

            def errors = importerService.validateParameters(importerProviderServiceParameters as ModelImporterProviderServiceParameters,
                                                            importer.importerProviderServiceParametersClass)

            if (errors.hasErrors()) {
                transactionStatus.setRollbackOnly()
                respond errors
                return
            }

            DataFlow dataFlow = importerService.importDataFlow(currentUser, importer, importerPluginParameters)

            if (!dataFlow) {
                transactionStatus.setRollbackOnly()
                return errorResponse(UNPROCESSABLE_ENTITY, 'No DataFlow imported')
            }

            dataFlow.validate()

            if (dataFlow.hasErrors()) {
                transactionStatus.setRollbackOnly()
                respond dataFlow.errors
                return
            }

            log.debug('No errors in imported DataFlow')

            dataFlow.save(flush: true, validate: false)

            log.debug('Saved models')

            respond dataFlow, status: CREATED, view: params.boolean('returnList') ? 'index' : 'show'
        }

        @Transactional
        def importDataFlows() throws ApiException {
            if (!isContextWriteable()) return unauthorised()

            DataFlowImporterPlugin importer = metadataCataloguePluginService.findDataFlowImporter(params.importerNamespace,
                                                                                                  params.importerName, params.importerVersion)
            if (!importer) return notFound("${params.importerNamespace}:${params.importerName}:${params.importerVersion}")

            if (!request.contentType.startsWith(MimeType.MULTIPART_FORM.name)) {
                return errorResponse(BAD_REQUEST, 'Request is not a multipart/form-data')
            }

            // Default through to importing single model
            // This may result in errors due to file containing multiple models, but that should be handled
            if (!importer.canImportMultipleDomains()) {
                params.returnList = true
                return importDataFlow()
            }

            // Otherwise import multiple models
            DataFlowImporterPluginParameters importerPluginParameters = importerService.extractImporterPluginParameters(
                importer, request as AbstractMultipartHttpServletRequest)

            def errors = importerService.validateParameters(importerPluginParameters, importer.importerPluginParametersClass)

            if (errors.hasErrors()) {
                transactionStatus.setRollbackOnly()
                respond errors
                return
            }

            List<DataFlow> result = importerService.importDataFlows(currentUser, importer, importerPluginParameters)

            if (!result) {
                transactionStatus.setRollbackOnly()
                return errorResponse(UNPROCESSABLE_ENTITY, 'No DataFlows imported')
            }

            result*.validate()

            if (result.any {it.hasErrors()}) {
                log.debug('Errors found in imported DataFlows')
                transactionStatus.setRollbackOnly()
                respond(getMultiErrorResponseMap(result), view: '/error', status: UNPROCESSABLE_ENTITY)
                return
            }

            log.debug('No errors in imported models')
            result*.save(flush: true, validate: false)
            log.debug('Saved all models')

            respond result, status: CREATED, view: 'index'

        }
    */

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
