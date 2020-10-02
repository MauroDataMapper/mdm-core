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
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetImporterProviderServiceParameters

import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 14/09/2020
 */
abstract class CodeSetImporterProviderService<T extends CodeSetImporterProviderServiceParameters>
    extends ImporterProviderService<CodeSet, T> {

    @Autowired
    CodeSetService codeSetService

    @Autowired
    ClassifierService classifierService

    @Override
    CodeSet importDomain(User currentUser, T params) {
        CodeSet codeSet = importCodeSet(currentUser, params)
        if (!codeSet) return null
        if (params.modelName) codeSet.label = params.modelName
        checkImport(currentUser, codeSet, params.finalised, params.importAsNewDocumentationVersion)
    }

    @Override
    List<CodeSet> importDomains(User currentUser, T params) {
        List<CodeSet> codeSets = importCodeSets(currentUser, params)
        codeSets?.collect { checkImport(currentUser, it, params.finalised, params.importAsNewDocumentationVersion) }
    }

    abstract CodeSet importCodeSet(User currentUser, T params)

    abstract List<CodeSet> importCodeSets(User currentUser, T params)

    @Override
    String getProviderType() {
        "CodeSet${ProviderType.IMPORTER.name}"
    }

    private CodeSet checkImport(User currentUser, CodeSet codeSet, boolean finalised, boolean importAsNewDocumentationVersion) {
        codeSetService.checkfinaliseModel(codeSet, finalised)
        codeSetService.checkDocumentationVersion(codeSet, importAsNewDocumentationVersion, currentUser)
        classifierService.checkClassifiers(currentUser, codeSet)

        codeSet
    }
}
