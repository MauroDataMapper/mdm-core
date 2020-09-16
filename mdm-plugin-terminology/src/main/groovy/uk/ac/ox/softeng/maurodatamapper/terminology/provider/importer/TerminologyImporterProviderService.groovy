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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 14/09/2020
 */
abstract class TerminologyImporterProviderService<T extends TerminologyImporterProviderServiceParameters>
    implements ImporterProviderService<Terminology, T> {

    @Autowired
    TerminologyService terminologyService

    @Autowired
    ClassifierService classifierService

    @Override
    Terminology importDomain(User currentUser, T params) {
        Terminology terminology = importTerminology(currentUser, params)
        if (!terminology) return null
        if (params.terminologyName) terminology.label = params.terminologyName
        checkImport(currentUser, terminology, params.finalised, params.importAsNewDocumentationVersion)
    }

    @Override
    List<Terminology> importDomains(User currentUser, T params) {
        List<Terminology> terminologies = importTerminologies(currentUser, params)
        terminologies?.collect { checkImport(currentUser, it, params.finalised, params.importAsNewDocumentationVersion) }
    }

    abstract Terminology importTerminology(User currentUser, T params)

    abstract List<Terminology> importTerminologies(User currentUser, T params)

    @Override
    String getProviderType() {
        "Terminology${ProviderType.IMPORTER.name}"
    }

    private Terminology checkImport(User currentUser, Terminology terminology, boolean finalised, boolean importAsNewDocumentationVersion) {
        terminologyService.checkfinaliseModel(terminology, finalised)
        terminologyService.checkDocumentationVersion(terminology, importAsNewDocumentationVersion, currentUser)
        classifierService.checkClassifiers(currentUser, terminology)

        terminology
    }
}
