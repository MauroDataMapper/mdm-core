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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.traits.controller.TerminologySecuredInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class TermInterceptor extends TerminologySecuredInterceptor {

    @Override
    Class getModelItemClass() {
        Term
    }

    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'termId')
        Utils.toUuid(params, 'codeSetId')
    }

    @Override
    void checkModelId() {
        if (!params.codeSetId && !params.terminologyId) {
            throw new ApiBadRequestException('TSI01', 'No TerminologyId or CodeSet provided against secured resource')
        }
    }

    boolean before() {
        performChecks()

        if (actionName in ['search', 'tree']) {
            return canReadModel()
        }

        if (isIndex() && params.containsKey('codeSetId')) {
            return checkActionAuthorisationOnUnsecuredResource(getModelItemClass(), params.id, CodeSet, params.codeSetId)
        }

        checkStandardActions()
    }
}
