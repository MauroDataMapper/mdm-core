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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationTypeService

class ReferenceEnumerationValueController extends CatalogueItemController<ReferenceEnumerationValue> {

    static responseFormats = ['json', 'xml']

    ReferenceEnumerationValueService referenceEnumerationValueService
    ReferenceEnumerationTypeService referenceEnumerationTypeService

    ReferenceEnumerationValueController() {
        super(ReferenceEnumerationValue)
    }

    @Override
    protected ReferenceEnumerationValue queryForResource(Serializable resourceId) {
        return referenceEnumerationValueService.findByIdAndReferenceEnumerationType(resourceId, params.referenceEnumerationTypeId ?: params.referenceDataTypeId)
    }

    @Override
    protected List<ReferenceEnumerationValue> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'idx'
        return referenceEnumerationValueService.findAllByReferenceEnumerationType(params.referenceEnumerationTypeId ?: params.referenceDataTypeId, params)
    }

    @Override
    void serviceDeleteResource(ReferenceEnumerationValue resource) {
        referenceEnumerationValueService.delete(resource)
    }

    @Override
    protected void serviceInsertResource(ReferenceEnumerationValue resource) {
        referenceEnumerationValueService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    protected ReferenceEnumerationValue createResource() {
        ReferenceEnumerationValue resource = super.createResource() as ReferenceEnumerationValue
        referenceEnumerationTypeService.get(params.referenceEnumerationTypeId ?: params.referenceDataTypeId)?.addToReferenceEnumerationValues(resource)
        resource
    }
}
