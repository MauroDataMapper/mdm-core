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
package uk.ac.ox.softeng.maurodatamapper.federation

import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES

class UrlMappings {

    static mappings = {
        group '/api', {
            /*
           ATOM feed
           */
            get "/feeds/all"(controller: 'feed', action: 'index')

            '/subscribedCatalogues'(resources: 'subscribedCatalogue') {
                get '/availableModels'(controller: 'subscribedCatalogue', action: 'publishedModels') // to be removed
                get '/publishedModels'(controller: 'subscribedCatalogue', action: 'publishedModels')
                get '/testConnection'(controller: 'subscribedCatalogue', action: 'testConnection')
                '/subscribedModels'(resources: 'subscribedModel', excludes: DEFAULT_EXCLUDES)
            }
            post "/subscribedModels/$subscribedModelId/federate"(controller: 'subscribedModel', action: 'federate')

            get '/published/models'(controller: 'publish', action: 'index')
        }
    }
}
