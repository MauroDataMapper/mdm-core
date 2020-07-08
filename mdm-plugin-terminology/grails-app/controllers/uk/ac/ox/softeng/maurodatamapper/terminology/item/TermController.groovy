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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.SearchService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService

class TermController extends CatalogueItemController<Term> {

    static responseFormats = ['json', 'xml']

    TermService termService
    CodeSetService codeSetService
    TerminologyService terminologyService

    SearchService searchService

    TermController() {
        super(Term)
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.searchTerm = searchParams.searchTerm ?: params.search
        params.max = params.max ?: searchParams.max ?: 10
        params.offset = params.offset ?: searchParams.offset ?: 0
        params.sort = params.sort ?: 'code'

        PaginatedLuceneResult<ModelItem> result = searchService.findAllByTerminologyIdByLuceneSearch(params.terminologyId, searchParams, params)

        respond result
    }

    def tree() {

        Terminology terminology = terminologyService.get(params.terminologyId)

        if (!terminology) return notFound(params.terminologyId, Terminology)

        if (!terminologyService.isTreeStructureCapableTerminology(terminology)) {
            return methodNotAllowed('Terminology does not support tree view')
        }

        List<ModelItemTreeItem> tree

        // If term id present then get tree of the termId
        if (params.containsKey('termId')) {

            Term term = queryForResource(params.termId)

            if (!term) return notFound(params.termId)

            tree = termService.buildTermTree(terminology, term)
        } else {
            // Otherwise get the top level tree
            tree = termService.buildTermTree(terminology)
        }

        respond tree
    }

    @Override
    protected Term queryForResource(Serializable resourceId) {
        if (params.codeSetId) {
            return termService.findByCodeSetIdAndId(params.codeSetId, resourceId)
        }

        termService.findByTerminologyIdAndId(params.terminologyId, resourceId)
    }

    @Override
    protected List<Term> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'code'
        if (params.classifierId) {
            return termService.findAllByClassifierId(params.classifierId, params)
        }
        if (params.codeSetId) {
            return termService.findAllByCodeSetId(params.codeSetId, params)
        }
        return termService.findAllByTerminologyId(params.terminologyId, params)
    }

    @Override
    void serviceDeleteResource(Term resource) {
        termService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(Term resource) {
        termService.save(resource)
    }

    @Override
    protected Term createResource() {
        Term resource = super.createResource() as Term
        terminologyService.get(params.terminologyId)?.addToTerms(resource)
        resource
    }

}
