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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.controller.ModelController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.CodeSetImporterProviderService

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

class CodeSetController extends ModelController<CodeSet> {
    static responseFormats = ['json', 'xml']

    CodeSetService codeSetService
    TermService termService
    TerminologyService terminologyService

    @Autowired(required = false)
    Set<CodeSetExporterProviderService> exporterProviderServices

    @Autowired(required = false)
    Set<CodeSetImporterProviderService> importerProviderServices

    CodeSetController() {
        super(CodeSet, 'codeSetId')
    }

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)

        if (params.termId) {
            if (!terminologyService.get(params.terminologyId)) {
                return notFound(Terminology, params.terminologyId)
            }
            if (!termService.get(params.termId)) {
                return notFound(Term, params.termId)
            }
        }

        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'index']
    }

    @Transactional
    def alterTerms() {
        CodeSet instance = queryForResource(params.codeSetId)

        if (instance == null) return notFound(params.codeSetId)

        Term term = termService.get(params.termId)

        if (!term) return notFound(Term, params.termId)

        if (request.method == 'PUT' || params.method == 'PUT') instance.addToTerms(term)
        else instance.removeFromTerms(term)

        updateResource instance

        updateResponse instance
    }

    @Override
    protected List<CodeSet> listAllReadableResources(Map params) {

        if (params.termId) {
            return codeSetService.findAllByTermIdAndUser(params.termId, currentUserSecurityPolicyManager, params)
        }
        super.listAllReadableResources(params)
    }

    @Override
    protected ModelService getModelService() {
        codeSetService
    }
}
