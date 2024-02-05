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
package uk.ac.ox.softeng.maurodatamapper.core.facet.rule

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.RuleService
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class RuleRepresentationInterceptor extends FacetInterceptor {

    RuleService ruleService

    @Override
    Class getFacetClass() {
        RuleRepresentation
    }

    boolean before() {
        facetResourceChecks()
        checkActionAllowedOnFacet()
    }

    @Override
    void checkAdditionalIds() {
        Utils.toUuid(params, 'ruleId')
    }

    @Override
    void checkParentId() throws ApiBadRequestException {
        if (!ruleService.existsByMultiFacetAwareItemIdAndId(params.multiFacetAwareItemId, params.ruleId)) {
            throw new ApiBadRequestException('AI01', 'Provided ruleId is not inside provided multiFacetAwareItemId')
        }
    }
}
