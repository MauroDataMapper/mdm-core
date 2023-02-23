/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer


import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelImporterProviderServiceParameters

import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 07/03/2018
 */
abstract class ReferenceDataModelImporterProviderService<T extends ReferenceDataModelImporterProviderServiceParameters>
    extends ModelImporterProviderService<ReferenceDataModel, T> {

    @Autowired
    ReferenceDataModelService referenceDataModelService

    @Override
    ModelService getModelService() {
        referenceDataModelService
    }

    @Override
    String getProviderType() {
        "ReferenceDataModel${ProviderType.IMPORTER.name}"
    }
}
