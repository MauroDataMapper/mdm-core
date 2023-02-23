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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.controller.DataModelSecuredInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class EnumerationValueInterceptor extends DataModelSecuredInterceptor {

    EnumerationTypeService enumerationTypeService

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'enumerationTypeId')
        Utils.toUuid(params, 'dataTypeId')
    }

    boolean before() {
        performChecks()
        checkStandardActions()
    }

    @Override
    Class getModelItemClass() {
        EnumerationValue
    }

    @Override
    void checkParentModelItemId() throws ApiBadRequestException {
        UUID dtId = params.enumerationTypeId ?: params.dataTypeId
        if (!enumerationTypeService.existsByDataModelIdAndId(params.dataModelId, dtId)) {
            throw new ApiBadRequestException('EVI01', 'Provided enumerationTypeId/dataTypeId is not inside provided dataModelId')
        }
    }
}