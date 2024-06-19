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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyTermData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.SearchService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

class TermController extends CatalogueItemController<Term> {

    static responseFormats = ['json', 'xml']

    @Autowired
    MessageSource messageSource

    TermService termService
    CodeSetService codeSetService
    TerminologyService terminologyService

    SearchService mdmPluginTerminologySearchService

    TermController() {
        super(Term)
    }

    def search() {
        SearchParams searchParams = SearchParams.bind(grailsApplication, getRequest())
        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.crossValuesIntoParametersMap(params, 'code')

        PaginatedHibernateSearchResult<ModelItem> result =
            mdmPluginTerminologySearchService.findAllByTerminologyIdByHibernateSearch(params.terminologyId,
                                                                                      searchParams,
                                                                                      params)

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

    @Transactional
    def copyTerm(CopyTermData copyTermData) {
        try {
            if (copyTermData.hasErrors()) {
                return errorResponse(HttpStatus.CONFLICT, "Validation error: \"${copyTermData.errors}\"")
            }
            Terminology terminology = params.containsKey('terminologyId')
                ? terminologyService.get(params.terminologyId)
                : null

            if (!terminology) {
                String idstr = params.containsKey('terminologyId')
                    ? params.terminologyId.toString()
                    : "(unknown)"
                return errorResponse(HttpStatus.NOT_FOUND, "Cannot find terminology: \"${idstr}\"")
            }
            Terminology targetTerminology = terminology

            if (copyTermData.targetTerminologyId != null) {
                targetTerminology = terminologyService.get(copyTermData.targetTerminologyId)

                if (!targetTerminology) {
                    return errorResponse(HttpStatus.NOT_FOUND, "Cannot find terminology: \"${copyTermData.targetTerminologyId}\"")
                }
            }
            Term term = terminology.terms.find {it.id == params.termId}

            if (!term) {
                return errorResponse(HttpStatus.NOT_FOUND, "Cannot find term: \"${params.termId}\"")
            }
            Term copy = termService.copyTerm(term, currentUser, currentUserSecurityPolicyManager)
            copy.code = copyTermData.code

            targetTerminology.addToTerms(copy)
            checkAndSave(messageSource, targetTerminology)

            return respond(copy)
        } catch(ValidationException e) {
            return errorResponse(HttpStatus.CONFLICT, "Validation error: \"${e.errors}\"")
        }
    }

    @Override
    protected List<Term> listAllResources(Map params) {
        log.trace('List all resources')
        // We do not want to allow unpaginated access to terms as there will be too many of them
        listAllReadableResources(params)
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
        termService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    protected Term createResource() {
        Term resource = super.createResource() as Term
        terminologyService.get(params.terminologyId)?.addToTerms(resource)
        resource
    }

}
