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
package uk.ac.ox.softeng.maurodatamapper.terminology


import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES_AND_NO_SAVE
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.INCLUDES_READ_ONLY

class UrlMappings {

    static mappings = {

        // provide plugin url mappings here
        group '/api', {

            group "/folders/$folderId", {
                get '/terminologies'(controller: 'terminology', action: 'index')
                put "/terminologies/$terminologyId"(controller: 'terminology', action: 'changeFolder')

                get '/codeSets'(controller: 'codeSet', action: 'index')
                put "/codeSets/$codeSetId"(controller: 'codeSet', action: 'changeFolder')

                // Allows us to control posting models into folders
                post "/terminologies"(controller: 'terminology', action: 'save') // new URL
                // Allows us to control posting models into folders
                post "/codeSets"(controller: 'codeSet', action: 'save') // new URL
            }

            '/terminologies'(resources: 'terminology', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {

                put '/finalise'(controller: 'terminology', action: 'finalise')
                put '/newDocumentationVersion'(controller: 'terminology', action: 'newDocumentationVersion')
                put '/newModelVersion'(controller: 'terminology', action: 'newModelVersion')

                get "/diff/$otherModelId"(controller: 'terminology', action: 'diff') // New URL
                put "/folder/$folderId"(controller: 'terminology', action: 'changeFolder')
                get "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'terminology', action: 'exportModel') // New URL

                put '/readByEveryone'(controller: 'terminology', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'terminology', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'terminology', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'terminology', action: 'readByAuthenticated')

                /*
                Terms
                 */
                '/terms'(resources: 'term', excludes: DEFAULT_EXCLUDES) {
                    '/termRelationships'(resources: 'termRelationship', excludes: DEFAULT_EXCLUDES)
                }

                post '/terms/search'(controller: 'term', action: 'search')
                get '/terms/search'(controller: 'term', action: 'search') // New URL
                get "/terms/tree/$termId?"(controller: 'term', action: 'tree') // New URL

                /*
                Term Relationship Types
                 */
                '/termRelationshipTypes'(resources: 'termRelationshipType', excludes: DEFAULT_EXCLUDES) {
                    '/termRelationships'(resources: 'termRelationship', includes: INCLUDES_READ_ONLY)
                }
            }

            group '/terminologies', {
                delete '/'(controller: 'terminology', action: 'deleteAll')

                group '/providers', {
                    get '/exporters'(controller: 'terminology', action: 'exporterProviders') // new url
                    get '/importers'(controller: 'terminology', action: 'importerProviders') // new url
                }

                post "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'terminology', action: 'exportModels')
                post "/import/$importerNamespace/$importerName/$importerVersion"(controller: 'terminology', action: 'importModels')
            }

            '/codeSets'(resources: 'codeSet', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {

                put '/finalise'(controller: 'codeSet', action: 'finalise')
                put '/newDocumentationVersion'(controller: 'codeSet', action: 'newDocumentationVersion')
                put '/newModelVersion'(controller: 'codeSet', action: 'newModelVersion')

                get "/diff/$otherModelId"(controller: 'codeSet', action: 'diff') // New URL
                put "/folder/$folderId"(controller: 'codeSet', action: 'changeFolder')
                get "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'codeSet', action: 'exportModel') // New URL

                put '/readByEveryone'(controller: 'codeSet', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'codeSet', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'codeSet', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'codeSet', action: 'readByAuthenticated')

                /*
                Terms
                 */
                get '/terms'(controller: 'term', action: 'index')
                put "/terms/$termId"(controller: 'codeSet', action: 'alterTerms')
                delete "/terms/$termId"(controller: 'codeSet', action: 'alterTerms')
            }

            group '/codeSets', {
                delete '/'(controller: 'codeSet', action: 'deleteAll')

                group '/providers', {
                    get '/exporters'(controller: 'codeSet', action: 'exporterProviders') // new url
                    get '/importers'(controller: 'codeSet', action: 'importerProviders') // new url
                }

                post "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'codeSet', action: 'exportModels')
                post "/import/$importerNamespace/$importerName/$importerVersion"(controller: 'codeSet', action: 'importModels')
            }
        }
    }
}
