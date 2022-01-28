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
package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

class MauroDataMapperServiceProviderController implements ResourcelessMdmController {
    static responseFormats = ['json', 'xml']

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService

    def importerProviders() {
        respond([importerProviders: mauroDataMapperServiceProviderService.importerProviderServices])
    }

    def dataLoaderProviders() {
        respond dataLoaderProviders: mauroDataMapperServiceProviderService.dataLoaderProviderServices
    }

    def emailProviders() {
        respond emailProviders: mauroDataMapperServiceProviderService.emailProviderServices
    }

    def exporterProviders() {
        respond([exporterProviders: mauroDataMapperServiceProviderService.exporterProviderServices])
    }
}
