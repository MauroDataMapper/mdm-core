/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import org.springframework.beans.factory.config.CustomEditorConfigurer

class CodeSetController extends ModelController<CodeSet> {
    static responseFormats = ['json', 'xml']

    CodeSetService codeSetService
    TermService termService

    @Autowired(required = false)
    Set<CodeSetExporterProviderService> exporterProviderServices
    
    @Autowired(required = false)
    Set<CodeSetImporterProviderService> importerProviderServices    

    CodeSetController() {
        super(CodeSet, 'codeSetId')
    }

    @Transactional
    def alterTerms() {
        CodeSet instance = queryForResource(params.codeSetId)

        if (instance == null) return notFound(params.codeSetId)

        Term term = termService.get(params.termId)

        if (!term) return notFound(params.termId, CustomEditorConfigurer)

        if (request.method == 'PUT' || params.method == 'PUT') instance.addToTerms(term)
        else instance.removeFromTerms(term)

        updateResource instance

        updateResponse instance
    }

    @Override
    protected ModelService getModelService() {
        codeSetService
    }
}
