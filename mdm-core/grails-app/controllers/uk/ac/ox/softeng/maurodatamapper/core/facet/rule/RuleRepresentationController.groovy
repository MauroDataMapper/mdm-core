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
package uk.ac.ox.softeng.maurodatamapper.core.facet.rule

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.facet.RuleService

class RuleRepresentationController extends EditLoggingController<RuleRepresentation> {

    static responseFormats = ['json', 'xml']

    RuleService ruleService
    RuleRepresentationService ruleRepresentationService

    RuleRepresentationController() {
        super(RuleRepresentation)
    }

    @Override
    protected RuleRepresentation queryForResource(Serializable resourceId) {
        return ruleRepresentationService.findByRuleIdAndId(params.ruleId, resourceId)
    }

    @Override
    protected List<RuleRepresentation> listAllReadableResources(Map params) {
        return ruleRepresentationService.findAllByRuleId(params.ruleId, params)
    }

    @Override
    protected RuleRepresentation createResource() {
        //Create the RuleRepresentation
        RuleRepresentation resource = super.createResource() as RuleRepresentation
        //Create an association between the Rule and RuleRepresentation
        ruleService.findByMultiFacetAwareItemIdAndId(params.multiFacetAwareItemId, params.ruleId)?.addToRuleRepresentations(resource)
        resource
    }

    @Override
    protected RuleRepresentation saveResource(RuleRepresentation resource) {
        resource.save flush: true, validate: false
        //Record the creation against the MultiFacetAwareItem to which the Rule owning the RuleRepresentation belongs
        ruleService.
            addCreatedEditToMultiFacetAwareItemOfRule(currentUser, resource, params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        resource
    }

    @Override
    protected RuleRepresentation updateResource(RuleRepresentation resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        //Record the update against the MultiFacetAwareItem to which the Rule owning the RuleRepresentation belongs
        ruleService.
            addUpdatedEditToMultiFacetAwareItemOfRule(currentUser, resource, params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId,
                                                      dirtyPropertyNames)
    }

    @Override
    void deleteResource(RuleRepresentation resource) {
        serviceDeleteResource(resource)
    }

    @Override
    void serviceDeleteResource(RuleRepresentation resource) {
        //Record the deletion against the MultiFacetAwareItem to which the Rule owning the RuleRepresentation belongs
        //(do this before deleting the association to rule, because edit logging needs to know the rule)
        ruleService.
            addDeletedEditToMultiFacetAwareItemOfRule(currentUser, resource, params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)

        //Delete the association between the Rule and RuleRepresentation
        ruleService.get(params.ruleId)?.removeFromRuleRepresentations(resource)

        //Delete the RuleRepresentation
        ruleRepresentationService.delete(resource, true)
    }
}
