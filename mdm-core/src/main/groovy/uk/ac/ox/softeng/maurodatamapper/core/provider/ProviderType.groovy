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
package uk.ac.ox.softeng.maurodatamapper.core.provider


import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService

import com.google.common.base.CaseFormat

/**
 * @since 22/08/2017
 */
enum ProviderType {

    DATALOADER(DataLoaderProviderService),
    EMAIL(EmailProviderService),
    IMPORTER(ImporterProviderService),
    EXPORTER(ExporterProviderService)
    //PROFILE(ProfilePlugin)

    Class<? extends MauroDataMapperService> pluginTypeClass

    ProviderType(Class<? extends MauroDataMapperService> clazz) {
        this.pluginTypeClass = clazz
    }

    Class<? extends MauroDataMapperService> getPluginTypeClass() {
        pluginTypeClass
    }

    String getTypeName() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name)
    }

    String getName() {
        pluginTypeClass.getSimpleName() - 'Service'
    }

    static List<String> getProviderTypeNames() {
        values().toList().collect {it.getTypeName()}
    }
}
