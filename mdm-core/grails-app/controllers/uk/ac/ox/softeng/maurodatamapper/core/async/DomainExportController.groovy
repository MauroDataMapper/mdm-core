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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.rest.RestfulController

/**
 * @since 18/05/2022
 */
class DomainExportController extends RestfulController<DomainExport> implements MdmController {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: [], update: [], patch: [], delete: ['DELETE'], show: 'GET', index: 'GET']

    DomainExportService domainExportService

    DomainExportController() {
        super(DomainExport)
    }

    @Override
    def index(Integer max) {
        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, view: 'index'
    }

    @Override
    def show() {
        DomainExport resource = queryForResource(params.id)
        resource ? respond(resource, view: 'show') : notFound(params.id)
    }

    def download() {
        DomainExport resource = queryForResource(params.domainExportId)

        if (!resource) {
            return notFound(params.domainExportId)
        }

        render(file: resource.exportData, fileName: resource.exportFileName, contentType: resource.exportContentType)
    }

    @Override
    protected List<DomainExport> listAllResources(Map params) {
        Map remappedParams = domainExportService.updatePaginationAndFilterParameters(params)
        if (remappedParams.resourceId) {
            if (remappedParams.exporterNamespace) {
                Version version = remappedParams.exporterVersion ? Version.from(remappedParams.exporterVersion) : null
                return domainExportService.findAllByExportedDomainAndExporterProviderService(
                    remappedParams.resourceId, remappedParams.resourceDomainType, remappedParams.exporterNamespace, remappedParams.exporterName, version, remappedParams
                )
            }
            return domainExportService.findAllByExportedDomain(remappedParams.resourceId, remappedParams.resourceDomainType, remappedParams)
        }


        currentUserSecurityPolicyManager.isApplicationAdministrator() ?
        domainExportService.listWithFilter(remappedParams, remappedParams) :
        domainExportService.findAllReadableByUser(currentUserSecurityPolicyManager, remappedParams)

    }

    @Override
    protected DomainExport queryForResource(Serializable id) {
        domainExportService.get(id)
    }
}
