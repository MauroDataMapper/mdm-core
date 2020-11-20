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

class RuleController extends EditLoggingController<Rule> {

    static responseFormats = ['json', 'xml']

    RuleService ruleService

    RuleController() {
        super(Rule)
    }

    @Override
    protected Rule queryForResource(Serializable resourceId) {
        return ruleService.findByCatalogueItemIdAndId(params.catalogueItemId, resourceId)
    }

    @Override
    protected List<Rule> listAllReadableResources(Map params) {
        return ruleService.findAllByCatalogueItemId(params.catalogueItemId, params)
    }

    @Override
    protected Rule createResource() {
        Rule resource = super.createResource() as Rule
        resource.clearErrors()
        resource.catalogueItem = ruleService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    void serviceDeleteResource(Rule resource) {
        ruleService.delete(resource, true)
    }

    @Override
    protected Rule saveResource(Rule resource) {
        resource.save flush: true, validate: false
        ruleService.addRuleToCatalogueItem(resource, resource.catalogueItem)
        ruleService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected Rule updateResource(Rule resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        ruleService.
            addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId, dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(Rule resource) {
        serviceDeleteResource(resource)
        ruleService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected boolean validateResource(Rule instance, String view) {
        ruleService.validate(instance)
        super.validateResource(instance, view)
    }
}
