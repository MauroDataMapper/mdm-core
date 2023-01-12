/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES

class UrlMappings {

    static mappings = {

        group '/api', {
            /*
             * the dataModelId should ALWAYS be the target DataModel for any action involving a write
             */
            "/dataModels/${dataModelId}/dataFlows"(resources: 'dataFlow', excludes: DEFAULT_EXCLUDES) {

                get "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'dataFlow', action: 'exportDataFlow')

                put '/diagramLayout'(controller: 'dataFlow', action: 'updateDiagramLayout')

                '/dataClassComponents'(resources: 'dataClassComponent', excludes: DEFAULT_EXCLUDES) {
                    '/dataElementComponents'(resources: 'dataElementComponent', excludes: DEFAULT_EXCLUDES) {
                        /**
                         * type = source|target
                         */
                        put "/${type}/$dataElementId"(controller: 'dataElementComponent', action: 'alterDataElements')
                        delete "/${type}/$dataElementId"(controller: 'dataElementComponent', action: 'alterDataElements')
                    }
                    /**
                     * type = source|target
                     */
                    put "/${type}/$dataClassId"(controller: 'dataClassComponent', action: 'alterDataClasses')
                    delete "/${type}/$dataClassId"(controller: 'dataClassComponent', action: 'alterDataClasses')
                }
            }

            group '/dataFlows', {
                group '/providers', {
                    get '/exporters'(controller: 'dataFlow', action: 'exporterProviders') // new url
                    get '/importers'(controller: 'dataFlow', action: 'importerProviders') // new url
                }
            }

            group "/dataModels/${dataModelId}/dataFlows", {
                post "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'dataFlow', action: 'exportDataFlows')
                post "/import/$importerNamespace/$importerName/$importerVersion"(controller: 'dataFlow', action: 'importDataFlows')
            }
        }
    }
}
