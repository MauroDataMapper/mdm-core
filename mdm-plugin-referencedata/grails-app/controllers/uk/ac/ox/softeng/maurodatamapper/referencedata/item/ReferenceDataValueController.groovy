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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService

import groovy.util.logging.Slf4j

@Slf4j
class ReferenceDataValueController extends EditLoggingController<ReferenceDataValue> {
    static responseFormats = ['json', 'xml']

    ReferenceDataModelService referenceDataModelService
    ReferenceDataValueService referenceDataValueService

    ReferenceDataValueController() {
        super(ReferenceDataValue)
    }

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def res = listAllResources(params)
        if (response.isCommitted()) return
        if (params.asRows) {
            respond res, [
                model: [
                    numberOfRows     : referenceDataValueService.countRowsByReferenceDataModelId(params.referenceDataModelId),
                    referenceDataRows: res,
                ],
                view : 'indexAsRows'
            ]
            return
        }
        respond res, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'index']
    }

    def search(SearchParams searchParams) {
        if (params.all) removePaginationParameters()

        //Always sort by rowNumber
        params.sortBy = 'rowNumber'

        //Paginate the distinct rowNumber selection or selection of values
        params.max = params.max ? Integer.parseInt(params.max) : searchParams.max ?: 10
        params.offset = params.offset ? Integer.parseInt(params.offset) : searchParams.offset ?: 0

        String searchTerm = params.search ?: searchParams.searchTerm ?: ''

        if (params.asRows) {
            List referenceDataRows = []

            //Get distinct rowNumbers which have a value matching the searchTerm
            List rowNumbers =
                referenceDataValueService.findDistinctRowNumbersByReferenceDataModelIdAndValueIlike(params.referenceDataModelId, searchTerm)

            if (rowNumbers.size() > 0) {
                //Make a list of referenceDataValues WHERE reference_data_model_id = params.referenceDataModelId AND rowNumber IN [rowNumbers] and
                // convert this into rows

                //Do our own pagination by choosing the relevant n items from our distinct rowNumbers
                int fromRow = params.offset
                if (fromRow > rowNumbers.size() - 1) {
                    fromRow = 0
                }

                int toRow = fromRow + params.max
                if (toRow > rowNumbers.size() - 1) {
                    toRow = rowNumbers.size() - 1
                }
                List rowNumbersToFetch = rowNumbers[fromRow..toRow]
                referenceDataRows =
                    rowify(referenceDataValueService.findAllByReferenceDataModelIdAndRowNumberIn(params.referenceDataModelId, rowNumbersToFetch))
            }

            respond referenceDataRows, [model: [numberOfRows: rowNumbers.size(), referenceDataRows:
                referenceDataRows, userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'searchAsRows']
        } else {
            //Make a list of referenceDataValues which have a value matching the search term
            List referenceDataValues =
                referenceDataValueService.findAllByReferenceDataModelIdAndValueIlike(params.referenceDataModelId, searchTerm, params)

            respond referenceDataValues,
                    [model: [referenceDataValues: referenceDataValues, userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'search']
        }
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
            Integer toRowNumber = Integer.MAX_VALUE
            if (params.max) {
                toRowNumber = fromRowNumber + params.max
            }

            //Now, we don't want to do normal pagination
            removePaginationParameters()

            //Make a list of referenceDataValues WHERE reference_data_model_id = params.referenceDataModelId AND rowNumber >= fromRowNumber AND
            // rowNumber < toRowNumber
            List referenceDataValues =
                referenceDataValueService.findAllByReferenceDataModelIdAndRowNumber(params.referenceDataModelId, fromRowNumber, toRowNumber, params)
            return rowify(referenceDataValues)
        }
        referenceDataValueService.findAllByReferenceDataModelId(params.referenceDataModelId, params)
    }

    @Override
    protected ReferenceDataValue createResource(Map includesExcludes = Collections.EMPTY_MAP) {
        ReferenceDataValue instance = super.createResource(includesExcludes)

        instance.referenceDataModel = referenceDataModelService.get(params.referenceDataModelId)

        instance
    }

    @Override
    protected ReferenceDataValue saveResource(ReferenceDataValue resource) {
        log.trace('save resource')
        resource.save(flush: true, validate: false)
        if (!params.boolean('noHistory')) {
            ReferenceDataElement owner = resource.referenceDataElement
            owner.addToEditsTransactionally(EditTitle.CREATE, currentUser, "[$resource.editLabel] added to component [$owner.editLabel]")
        }
        resource
    }

    @Override
    protected ReferenceDataValue updateResource(ReferenceDataValue resource) {
        log.trace('update {}', resource.ident())
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save(flush: true, validate: false)
        if (!params.boolean('noHistory')) {
            ReferenceDataElement owner = resource.referenceDataElement
            owner.addToEditsTransactionally(EditTitle.UPDATE, currentUser, resource.editLabel, dirtyPropertyNames)
        }
        resource
    }

    @Override
    protected void serviceDeleteResource(ReferenceDataValue resource) {
        referenceDataValueService.delete(resource, true)
    }

    /**
     * Turn a list of ReferenceDataValue into a list of rows
     */
    private List rowify(List<ReferenceDataValue> referenceDataValues) {
        //Sort the list by rowNumber ascending and then columnNumber ascending
        referenceDataValues.sort {it1, it2 ->
            it1.rowNumber <=> it2.rowNumber ?: it1.referenceDataElement.columnNumber <=> it2.referenceDataElement.columnNumber
        }

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
        rowNumbers.eachWithIndex {rowNumber, i ->
            List referenceDataRow = []
            referenceDataValues.each {
                if (it.rowNumber == rowNumber) {
                    referenceDataRow.add(it)
                }
            }
            referenceDataRows.add(referenceDataRow)
        }

        return referenceDataRows
    }
}
