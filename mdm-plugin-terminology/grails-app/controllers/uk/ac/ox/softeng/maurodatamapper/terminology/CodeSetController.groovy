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
package uk.ac.ox.softeng.maurodatamapper.terminology


import uk.ac.ox.softeng.maurodatamapper.core.controller.ModelController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService

import grails.gorm.transactions.Transactional
import grails.web.http.HttpHeaders
import org.springframework.beans.factory.config.CustomEditorConfigurer

import static org.springframework.http.HttpStatus.OK

class CodeSetController extends ModelController<CodeSet> {
    static responseFormats = ['json', 'xml']

    CodeSetService codeSetService
    TermService termService

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

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [classMessageArg, instance.id])
                redirect instance
            }
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond instance, [status: OK, view: 'update']
            }
        }
    }

    @Override
    protected ModelService getModelService() {
        codeSetService
    }

    @Override
    Set<ExporterProviderService> getExporterProviderServices() {
        [] as Set
    }

    @Override
    Set<ImporterProviderService> getImporterProviderServices() {
        [] as Set
    }
}
