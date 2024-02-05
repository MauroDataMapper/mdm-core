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
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.TerminologyExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyImporterProviderService

import org.springframework.beans.factory.annotation.Autowired

class TerminologyController extends ModelController<Terminology> {

    static responseFormats = ['json', 'xml']

    TerminologyService terminologyService

    @Autowired(required = false)
    Set<TerminologyExporterProviderService> exporterProviderServices

    @Autowired(required = false)
    Set<TerminologyImporterProviderService> importerProviderServices

    TerminologyController() {
        super(Terminology, 'terminologyId')
    }

    @Override
    protected ModelService<Terminology> getModelService() {
        terminologyService
    }

}
