/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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


import uk.ac.ox.softeng.maurodatamapper.core.controller.FacetController
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService

import grails.gorm.transactions.Transactional

class SemanticLinkController extends FacetController<SemanticLink> {

    static responseFormats = ['json', 'xml']

    SemanticLinkService semanticLinkService

    SemanticLinkController() {
        super(SemanticLink)
    }

    @Override
    MultiFacetItemAwareService getFacetService() {
        semanticLinkService
    }

    @Transactional
    def confirm() {
        if (handleReadOnly()) return

        SemanticLink instance = queryForResource(params.semanticLinkId)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        instance.unconfirmed = false

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Override
    protected SemanticLink queryForResource(Serializable id) {
        SemanticLink resource = super.queryForResource(id) as SemanticLink
        semanticLinkService.loadMultiFacetAwareItemsIntoSemanticLink(resource)
    }

    @Override
    protected List<SemanticLink> listAllReadableResources(Map params) {
        List<SemanticLink> semanticLinks
        switch (params.type) {
            case 'source':
                semanticLinks = semanticLinkService.findAllBySourceMultiFacetAwareItemId(params.multiFacetAwareItemId, params)
                break
            case 'target':
                semanticLinks = semanticLinkService.findAllByTargetMultiFacetAwareItemId(params.multiFacetAwareItemId, params)
                break
            default:
                semanticLinks = semanticLinkService.findAllBySourceOrTargetMultiFacetAwareItemId(params.multiFacetAwareItemId, params)
        }
        semanticLinkService.loadMultiFacetAwareItemsIntoSemanticLinks(semanticLinks)
    }

    @Override
    protected SemanticLink saveResource(SemanticLink resource) {
        semanticLinkService.loadMultiFacetAwareItemsIntoSemanticLink(resource)
        super.saveResource(resource)
    }

    @Override
    protected SemanticLink updateResource(SemanticLink resource) {
        semanticLinkService.loadMultiFacetAwareItemsIntoSemanticLink(resource)
        super.updateResource(resource)
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
