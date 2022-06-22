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
package uk.ac.ox.softeng.maurodatamapper.profile

class UrlMappings {

    static mappings = {
        group '/api', {

            group '/profiles', {
                get '/providers'(controller: 'profile', action: 'profileProviders')
                get '/providers/dynamic'(controller: 'profile', action: 'dynamicProfileProviders')

                group "/$profileNamespace/$profileName", {

                    get '/'(controller: 'profile', action: 'emptyProfile')

                    // New URL replaces /api/profiles/namespace/name/customSearch
                    post '/search'(controller: 'profile', action: 'search')
                    // New URL replaces /api/dataModels/profile/namespace/name/version
                    get "/$multiFacetAwareItemDomainType"(controller: 'profile', action: 'listModelsInProfile')

                    // New URL replaces /api/dataModels/profile/values/namespace/name/version
                    get "/${multiFacetAwareItemDomainType}/values"(controller: 'profile', action: 'listValuesInProfile')

                    group "/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}", {
                        // Provide multiple ways to obtain profile of a multiFacetAware
                        get '/'(controller: 'profile', action: 'show')
                        post '/'(controller: 'profile', action: 'save')
                        post '/validate'(controller: 'profile', action: 'validate')
                        delete '/'(controller: 'profile', action: 'delete')
                        post '/search'(controller: 'profile', action: 'search')
                    }
                }
            }

            // Provide multiple ways to obtain profile of a multiFacetAware
            group "/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}", {
                group '/profiles', {
                    get '/used'(controller: 'profile', action: 'usedProfiles')
                    get '/unused'(controller: 'profile', action: 'unusedProfiles')
                    get '/otherMetadata'(controller: 'profile', action: 'nonProfileMetadata')
                    get '/nonProfileMetadata'(controller: 'profile', action: 'nonProfileMetadata')
                    post "/$profileNamespace/$profileName/search"(controller: 'profile', action: 'search')
                }

                group '/profile', {
                    group "/$profileNamespace/$profileName/$profileVersion?", {
                        get '/'(controller: 'profile', action: 'show')
                        delete '/'(controller: 'profile', action: 'delete')
                        post '/'(controller: 'profile', action: 'save')
                    }

                    post "/$profileNamespace/$profileName/$profileVersion/validate"(controller: 'profile', action: 'validate')
                    post "/$profileNamespace/$profileName/validate"(controller: 'profile', action: 'validate')
                }
            }

            // Methods to retrieve and save many profiles for many multiFacetAware items belonging to a Model
            group "/${modelDomainType}/${modelId}", {
                post '/profile/getMany'(controller: 'profile', action: 'getMany')
                post '/profile/validateMany'(controller: 'profile', action: 'validateMany')
                post '/profile/saveMany'(controller: 'profile', action: 'saveMany')
            }

            Closure importEndpoints = {
                get '/used'(controller: 'profile', action: 'usedProfiles')
                get '/unused'(controller: 'profile', action: 'unusedProfiles')
                group "/$profileNamespace/$profileName", {
                    get '/'(controller: 'profile', action: 'show')
                    delete '/'(controller: 'profile', action: 'delete')
                    post '/'(controller: 'profile', action: 'save')
                }
                post "/$profileNamespace/$profileName/validate"(controller: 'profile', action: 'validate')
            }

            group "/dataModels/$dataModelId", {
                group "/dataTypes/$importedDataTypeId/import/profiles", importEndpoints
                group "/dataClasses/$importedDataClassId/import/profiles", importEndpoints
                group "/dataClasses/$dataClassId/dataClasses/$importedDataClassId/import/profiles", importEndpoints
                group "/dataClasses/$dataClassId/dataElements/$importedDataElementId/import/profiles", importEndpoints
            }
        }
    }
}