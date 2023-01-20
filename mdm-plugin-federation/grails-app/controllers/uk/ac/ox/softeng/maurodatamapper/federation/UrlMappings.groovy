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
package uk.ac.ox.softeng.maurodatamapper.federation

import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES_AND_NO_SAVE

class UrlMappings {

    static mappings = {
        group '/api', {
            /*
           ATOM feed
           */
            get '/feeds/all'(controller: 'feed', action: 'index')

            group '/admin', {
                '/subscribedCatalogues'(resources: 'subscribedCatalogue', excludes: DEFAULT_EXCLUDES) {
                    get '/testConnection'(controller: 'subscribedCatalogue', action: 'testConnection')
                }
                group '/subscribedCatalogues', {
                    get '/types'(controller: 'subscribedCatalogue', action: 'types')
                    get '/authenticationTypes'(controller: 'subscribedCatalogue', action: 'authenticationTypes')
                }
            }

            group '/subscribedCatalogues', {
                get '/types'(controller: 'subscribedCatalogue', action: 'types')
                get '/authenticationTypes'(controller: 'subscribedCatalogue', action: 'authenticationTypes')
                get '/'(controller: 'subscribedCatalogue', action: 'index') {
                    openAccess = true
                }
                group "/$subscribedCatalogueId", {
                    get '/'(controller: 'subscribedCatalogue', action: 'show') {
                        openAccess = true
                    }
                    get '/testConnection'(controller: 'subscribedCatalogue', action: 'testConnection')
                    get '/publishedModels'(controller: 'subscribedCatalogue', action: 'publishedModels')
                    get "/publishedModels/$publishedModelId/newerVersions"(controller: 'subscribedCatalogue', action: 'newerVersions')
                    '/subscribedModels'(resources: 'subscribedModel', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE)
                    post '/subscribedModels'(controller: 'subscribedModel', action: 'federate')
                    get "/subscribedModels/$id/newerVersions"(controller: 'subscribedModel', action: 'newerVersions')
                }
            }

            get '/published/models'(controller: 'publish', action: 'index')
            get "/published/models/$publishedModelId/newerVersions"(controller: 'publish', action: 'newerVersions')
        }
    }
}
