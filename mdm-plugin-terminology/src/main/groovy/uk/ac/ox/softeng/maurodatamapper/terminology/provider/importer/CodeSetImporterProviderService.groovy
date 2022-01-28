/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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


import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetImporterProviderServiceParameters

import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 14/09/2020
 */
abstract class CodeSetImporterProviderService<T extends CodeSetImporterProviderServiceParameters>
    extends ModelImporterProviderService<CodeSet, T> {

    @Autowired
    CodeSetService codeSetService

    @Override
    ModelService getModelService() {
        codeSetService
    }

    @Override
    String getProviderType() {
        "CodeSet${ProviderType.IMPORTER.name}"
    }
}
