/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService

class TermRelationshipTypeController extends CatalogueItemController<TermRelationshipType> {

    static responseFormats = ['json', 'xml']

    TerminologyService terminologyService
    TermRelationshipTypeService termRelationshipTypeService

    TermRelationshipTypeController() {
        super(TermRelationshipType)
    }

    @Override
    protected TermRelationshipType queryForResource(Serializable id) {
        termRelationshipTypeService.findByTerminologyIdAndId(params.terminologyId, id)
    }

    @Override
    protected List listAllReadableResources(Map params) {
        termRelationshipTypeService.findAllByTerminologyId(params.terminologyId, params)
    }

    @Override
    void serviceDeleteResource(TermRelationshipType resource) {
        termRelationshipTypeService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(TermRelationshipType resource) {
        termRelationshipTypeService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    protected TermRelationshipType createResource() {
        TermRelationshipType resource = super.createResource() as TermRelationshipType
        resource.clearErrors()
        terminologyService.get(params.terminologyId)?.addToTermRelationshipTypes(resource)
        resource
    }
}
