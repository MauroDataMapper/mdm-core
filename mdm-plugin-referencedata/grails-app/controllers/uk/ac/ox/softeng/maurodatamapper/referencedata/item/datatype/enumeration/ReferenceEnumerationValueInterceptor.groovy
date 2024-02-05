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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.traits.controller.ReferenceDataModelSecuredInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ReferenceEnumerationValueInterceptor extends ReferenceDataModelSecuredInterceptor {

    ReferenceEnumerationTypeService referenceEnumerationTypeService

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'referenceEnumerationTypeId')
        Utils.toUuid(params, 'referenceDataTypeId')
    }

    boolean before() {
        performChecks()
        checkStandardActions()
    }

    @Override
    Class getModelItemClass() {
        ReferenceEnumerationValue
    }

    @Override
    void checkParentModelItemId() throws ApiBadRequestException {
        UUID dtId = params.referenceEnumerationTypeId ?: params.referenceDataTypeId
        if (!referenceEnumerationTypeService.existsByReferenceDataModelIdAndId(params.referenceDataModelId, dtId)) {
            throw new ApiBadRequestException('REVI01', 'Provided referenceEnumerationTypeId/referenceDataTypeId is not inside provided referenceDataModelId')
        }
    }
}