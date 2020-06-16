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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

class VersionLinkController extends EditLoggingController<VersionLink> {

    static responseFormats = ['json', 'xml']

    VersionLinkService versionLinkService

    VersionLinkController() {
        super(VersionLink)
    }

    @Override
    protected VersionLink queryForResource(Serializable id) {
        VersionLink resource = super.queryForResource(id) as VersionLink
        versionLinkService.loadModelsIntoVersionLink(resource)
    }

    @Override
    protected List<VersionLink> listAllReadableResources(Map params) {
        List<VersionLink> versionLinks
        switch (params.type) {
            case 'source':
                versionLinks = versionLinkService.findAllBySourceModelId(params.modelId, params)
                break
            case 'target':
                versionLinks = versionLinkService.findAllByTargetModelId(params.modelId, params)
                break
            default:
                versionLinks = versionLinkService.findAllBySourceOrTargetModelId(params.modelId, params)
        }
        versionLinkService.loadModelsIntoVersionLinks(versionLinks)
    }

    @Override
    void serviceDeleteResource(VersionLink resource) {
        versionLinkService.delete(resource)
    }

    @Override
    protected VersionLink createResource() {
        VersionLink resource = super.createResource() as VersionLink
        resource.catalogueItem = versionLinkService.findCatalogueItemByDomainTypeAndId(params.modelDomainType, params.modelId)
        resource
    }

    @Override
    protected VersionLink saveResource(VersionLink resource) {
        versionLinkService.loadModelsIntoVersionLink(resource)
        resource.save flush: true, validate: false
        versionLinkService.addCreatedEditToCatalogueItem(currentUser, resource, params.modelDomainType, params.modelId)
    }

    @Override
    protected VersionLink updateResource(VersionLink resource) {
        versionLinkService.loadModelsIntoVersionLink(resource)
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        versionLinkService.addUpdatedEditToCatalogueItem(currentUser, resource, params.modelDomainType, params.modelId, dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(VersionLink resource) {
        serviceDeleteResource(resource)
        versionLinkService.addDeletedEditToCatalogueItem(currentUser, resource, params.modelDomainType, params.modelId)
    }
}
