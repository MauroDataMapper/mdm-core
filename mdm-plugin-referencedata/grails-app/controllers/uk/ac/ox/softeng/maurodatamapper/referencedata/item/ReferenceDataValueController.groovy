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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController

import groovy.util.logging.Slf4j

@Slf4j
class ReferenceDataValueController extends CatalogueItemController<ReferenceDataValue> {
    static responseFormats = ['json', 'xml']

    ReferenceDataValueService referenceDataValueService
    //ReferenceDataElementService referenceDataElementService

    ReferenceDataValueController() {
        super(ReferenceDataValue)
    }

    /**
     * Return ReferenceDataValues in one of two modes. If parameter 'asRows' is not set then simply return a List of ReferenceDataValue
     * with pagination done in the usual way.
     * If parameter 'asRows' is set then structure the List of ReferenceDataValue like rows in a table. In this case do not use
     * the usual pagination, but do a pseudo-pagination by rowNumber instead.
     */
    @Override
    protected List<ReferenceDataValue> listAllReadableResources(Map params) {
        if (params.all) removePaginationParameters()
        
        //Always sort by rowNumber
        params.sortBy = 'rowNumber'
        if (params.asRows) {
            //Default to starting with rowNumber 1, but adjust this if offset is set
            Integer fromRowNumber = 1
            if (params.offset) {
                fromRowNumber = Integer.parseInt(params.offset) + 1
            }

            //Default to returning all rows, but adjust this if max is set
            Integer toRowNumber = Integer.MAX_VALUE;
            if (params.max) {
                toRowNumber = fromRowNumber + params.max
            }

            //Now, we don't want to do normal pagination
            removePaginationParameters()

            //Make a list of referenceDataValues WHERE reference_data_model_id = params.referenceDataModelId AND rowNumber >= fromRowNumber AND rowNumber < toRowNumber
            List referenceDataValues = referenceDataValueService.findAllByReferenceDataModelIdAndRowNumber(params.referenceDataModelId, fromRowNumber, toRowNumber, params)
            
            //Sort the list by rowNumber ascending
            referenceDataValues.sort{it.getProperty(params.sortBy)}

            //Make a list of row numbers
            List rowNumbers = []
            referenceDataValues.each {
                if (!rowNumbers.contains(it.rowNumber)) {
                    rowNumbers.add(it.rowNumber)
                }
            }

            //Make a List containing rows
            List referenceDataRows = []
            //Make a row for each row number
            rowNumbers.eachWithIndex { rowNumber, i ->
                List referenceDataRow = []
                referenceDataValues.each {
                    if (it.rowNumber == rowNumber) {
                        referenceDataRow.add(it)
                    }
                }
                referenceDataRows.add(referenceDataRow)
            }

            respond referenceDataRows, [model: [numberOfRows: referenceDataValueService.countRowsByReferenceDataModelId(params.referenceDataModelId), referenceDataRows: referenceDataRows, userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'indexAsRows']
        } else {            
            return referenceDataValueService.findAllByReferenceDataModelId(params.referenceDataModelId, params)
        }
    }

    @Override
    void serviceDeleteResource(ReferenceDataValue resource) {
        //referenceDataValueService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(ReferenceDataValue resource) {
        //referenceDataValueService.save(flush: true, resource)
    }
}
