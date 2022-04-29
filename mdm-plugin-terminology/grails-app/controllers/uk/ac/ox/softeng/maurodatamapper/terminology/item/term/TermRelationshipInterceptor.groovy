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
package uk.ac.ox.softeng.maurodatamapper.terminology.item.term

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.traits.controller.TerminologySecuredInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class TermRelationshipInterceptor extends TerminologySecuredInterceptor {

    TermService termService
    TermRelationshipTypeService termRelationshipTypeService

    @Override
    Class getModelItemClass() {
        TermRelationship
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'termId')
        Utils.toUuid(params, 'termRelationshipTypeId')
    }

    @Override
    void checkParentModelItemId() throws ApiBadRequestException {
        if (params.containsKey('termId') && !termService.existsByTerminologyIdAndId(params.terminologyId, params.termId)) {
            throw new ApiBadRequestException('DEI01', 'Provided termId is not inside provided terminology')
        }
        if (params.containsKey('termRelationshipTypeId') && !termRelationshipTypeService.existsByTerminologyIdAndId(params.terminologyId, params.termRelationshipTypeId)) {
            throw new ApiBadRequestException('DEI01', 'Provided termRelationshipTypeId is not inside provided terminology')
        }
    }

    boolean before() {
        performChecks()
        checkStandardActions()
    }
}
