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
package uk.ac.ox.softeng.maurodatamapper.terminology.item.term

import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController

import grails.gorm.transactions.Transactional

class TermRelationshipController extends CatalogueItemController<TermRelationship> {

    static responseFormats = ['json', 'xml']

    TermRelationshipService termRelationshipService

    TermRelationshipController() {
        super(TermRelationship)
    }

    @Override
    protected TermRelationship queryForResource(Serializable id) {
        if (params.termId) {
            return termRelationshipService.findByTermIdAndId(params.termId, id)
        }
        if (params.termRelationshipTypeId) {
            return termRelationshipService.findByRelationshipTypeIdAndId(params.termRelationshipTypeId, id)
        }
        null
    }

    @Override
    protected List listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'dateCreated'

        if (params.type) {
            return termRelationshipService.findAllByTermIdAndType(params.termId, params.type, params)
        }
        if (params.termRelationshipTypeId) {
            return termRelationshipService.findAllByRelationshipTypeId(params.termRelationshipTypeId, params)
        }

        termRelationshipService.findAllByTermId(params.termId, params)
    }

    @Override
    void serviceDeleteResource(TermRelationship resource) {
        termRelationshipService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(TermRelationship resource) {
        termRelationshipService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    @Transactional
    protected boolean validateResource(TermRelationship instance, String view) {
        TermRelationship validated = termRelationshipService.validate(instance)

        // Check that the termId parameter is one of the terms used in the json body
        if (params.termId !in [validated.sourceTerm?.id, validated.targetTerm?.id]) {
            validated.errors.rejectValue('sourceTerm',
                                         'invalid.relationship.different.terms',
                                         'Term Relationship must define one of its Terms as the termId requested')
        }

        if (validated.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond validated.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }
}
