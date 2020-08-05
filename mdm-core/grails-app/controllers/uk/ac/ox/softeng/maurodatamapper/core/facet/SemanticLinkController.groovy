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

import grails.gorm.transactions.Transactional

class SemanticLinkController extends EditLoggingController<SemanticLink> {

    static responseFormats = ['json', 'xml']

    SemanticLinkService semanticLinkService

    SemanticLinkController() {
        super(SemanticLink)
    }

    @Override
    protected SemanticLink queryForResource(Serializable id) {
        SemanticLink resource = super.queryForResource(id) as SemanticLink
        semanticLinkService.loadCatalogueItemsIntoSemanticLink(resource)
    }

    @Override
    protected List<SemanticLink> listAllReadableResources(Map params) {
        List<SemanticLink> semanticLinks
        switch (params.type) {
            case 'source':
                semanticLinks = semanticLinkService.findAllBySourceCatalogueItemId(params.catalogueItemId, params)
                break
            case 'target':
                semanticLinks = semanticLinkService.findAllByTargetCatalogueItemId(params.catalogueItemId, params)
                break
            default:
                semanticLinks = semanticLinkService.findAllBySourceOrTargetCatalogueItemId(params.catalogueItemId, params)
        }
        semanticLinkService.loadCatalogueItemsIntoSemanticLinks(semanticLinks)
    }

    @Override
    void serviceDeleteResource(SemanticLink resource) {
        semanticLinkService.delete(resource)
    }

    @Override
    protected SemanticLink createResource() {
        SemanticLink resource = super.createResource() as SemanticLink
        resource.catalogueItem = semanticLinkService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    protected SemanticLink saveResource(SemanticLink resource) {
        semanticLinkService.loadCatalogueItemsIntoSemanticLink(resource)
        resource.save flush: true, validate: false
        semanticLinkService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected SemanticLink updateResource(SemanticLink resource) {
        semanticLinkService.loadCatalogueItemsIntoSemanticLink(resource)
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        semanticLinkService.
            addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId, dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(SemanticLink resource) {
        serviceDeleteResource(resource)
        semanticLinkService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Transactional
    @Override
    protected boolean validateResource(SemanticLink instance, String view) {
        // Ensure only assignable link types are constructed via the controller
        if (instance.linkType && !instance.linkType.isAssignable) {
            instance.errors.rejectValue('linkType',
                                        'semanticlink.linktype.must.be.assignable.message',
                                        ['linkType', SemanticLink, instance.linkType].toArray(),
                                        'Property [{0}] of class [{1}] with value [{2}] cannot be used')
        }
        if (instance.hasErrors() || !instance.validate()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }
}
