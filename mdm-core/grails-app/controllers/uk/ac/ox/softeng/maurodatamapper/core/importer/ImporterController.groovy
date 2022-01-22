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
package uk.ac.ox.softeng.maurodatamapper.core.importer

import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

import static org.springframework.http.HttpStatus.BAD_REQUEST

class ImporterController implements ResourcelessMdmController {

    static responseFormats = ['json', 'xml']

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    ImporterService importerService

    def parameters() {
        if (!params.ns || !params.name || !params.version) {
            return errorResponse(BAD_REQUEST, 'Namespace, name and version must be provided to identify individual importers')
        }

        ImporterProviderService importer = mauroDataMapperServiceProviderService.findImporterProvider(params.ns, params.name, params.version)

        if (!importer) return notFound(ImporterProviderService, "${params.ns}:${params.name}:${params.version}")

        respond importerProviderService: importer, importParameterGroups: importerService.describeImporterParams(importer)
    }
}
