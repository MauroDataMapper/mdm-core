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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationTypeService

class EnumerationValueController extends CatalogueItemController<EnumerationValue> {

    static responseFormats = ['json', 'xml']

    EnumerationValueService enumerationValueService
    EnumerationTypeService enumerationTypeService

    EnumerationValueController() {
        super(EnumerationValue)
    }

    @Override
    protected EnumerationValue queryForResource(Serializable resourceId) {
        return enumerationValueService.findByIdAndEnumerationType(resourceId, params.enumerationTypeId ?: params.dataTypeId)
    }

    @Override
    protected List<EnumerationValue> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'idx'
        return enumerationValueService.findAllByEnumerationType(params.enumerationTypeId ?: params.dataTypeId, params)
    }

    @Override
    void serviceDeleteResource(EnumerationValue resource) {
        enumerationValueService.delete(resource)
    }

    @Override
    protected void serviceInsertResource(EnumerationValue resource) {
        enumerationValueService.save(flush: true, resource)
    }

    @Override
    protected EnumerationValue createResource() {
        EnumerationValue resource = super.createResource() as EnumerationValue
        enumerationTypeService.get(params.enumerationTypeId ?: params.dataTypeId)?.addToEnumerationValues(resource)
        resource
    }
}
