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
package uk.ac.ox.softeng.maurodatamapper.core


import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES_AND_NO_UPDATE
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.INCLUDES_INDEX_ONLY
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.INCLUDES_READ_ONLY

class UrlMappings {

    static mappings = {


        '500'(view: '/error')
        '404'(view: '/notFound')
        '410'(view: '/gone')
        '501'(view: '/notImplemented')
        '401'(view: '/unauthorised')
        '400'(view: '/badRequest')

        group '/api', {

            group '/admin', {
                get '/modules'(controller: 'mauroDataMapperProvider', action: 'modules')
                get '/properties'(controller: 'admin', action: 'apiProperties')
                post '/rebuildLuceneIndexes'(controller: 'admin', action: 'rebuildLuceneIndexes')
                post '/editProperties'(controller: 'admin', action: 'editApiProperties')
                get '/status'(controller: 'admin', action: 'status')
                get '/activeSessions'(controller: 'session', action: 'activeSessions')
                '/emails'(resources: 'email', includes: INCLUDES_INDEX_ONLY)

                group "/tree/$containerDomainType/$modelDomainType", {
                    get '/documentationSuperseded'(controller: 'treeItem', action: 'documentationSupersededModels') // New URL
                    get '/modelSuperseded'(controller: 'treeItem', action: 'modelSupersededModels') // New URL
                    get '/deleted'(controller: 'treeItem', action: 'deletedModels') // New URL
                }

                group '/providers', {
                    get '/importers'(controller: 'mauroDataMapperServiceProvider', action: 'importerProviders')
                    get '/dataLoaders'(controller: 'mauroDataMapperServiceProvider', action: 'dataLoaderProviders')
                    get '/emailers'(controller: 'mauroDataMapperServiceProvider', action: 'emailProviders')
                    get '/exporters'(controller: 'mauroDataMapperServiceProvider', action: 'exporterProviders')
                }
            }

            // Open access url
            get "/session/isAuthenticated/$sesssionId?"(controller: 'session', action: 'isAuthenticatedSession') // New Url
            get '/session/isApplicationAdministration'(controller: 'session', action: 'isApplicationAdministrationSession') // New Url
            get '/session/keepAlive'(controller: 'session', action: 'keepAlive') // New Url

            group '/importer', {
                get "/parameters/$ns?/$name?/$version?"(controller: 'importer', action: 'parameters')
            }

            '/folders'(resources: 'folder', excludes: DEFAULT_EXCLUDES) {
                '/folders'(resources: 'folder', excludes: DEFAULT_EXCLUDES)
                '/versionedFolders'(resources: 'versionedFolder', excludes: DEFAULT_EXCLUDES)

                put '/readByEveryone'(controller: 'folder', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'folder', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'folder', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'folder', action: 'readByAuthenticated')

                post '/search'(controller: 'folder', action: 'search')
                get '/search'(controller: 'folder', action: 'search')
            }

            '/versionedFolders'(resources: 'versionedFolder', excludes: DEFAULT_EXCLUDES) {
                '/folders'(resources: 'folder', excludes: DEFAULT_EXCLUDES)
                '/versionedFolders'(resources: 'versionedFolder', excludes: DEFAULT_EXCLUDES)

                put '/readByEveryone'(controller: 'versionedFolder', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'versionedFolder', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'versionedFolder', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'versionedFolder', action: 'readByAuthenticated')

                post '/search'(controller: 'versionedFolder', action: 'search')
                get '/search'(controller: 'versionedFolder', action: 'search')
            }

            '/classifiers'(resources: 'classifier', excludes: DEFAULT_EXCLUDES) {
                '/classifiers'(resources: 'classifier', excludes: DEFAULT_EXCLUDES)
                get '/catalogueItems'(controller: 'classifier', action: 'catalogueItems') // New URL
                put '/readByEveryone'(controller: 'classifier', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'classifier', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'classifier', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'classifier', action: 'readByAuthenticated')
            }

            '/authorities'(resources: 'authority', includes: INCLUDES_READ_ONLY)

            /*
             Full Searching
             */
            group '/catalogueItems', {
                post '/search'(controller: 'search', action: 'search')
                get '/search'(controller: 'search', action: 'search')
            }

            /*
            Catalogue Item accessible resources
            All new URLs
             */
            group "/$catalogueItemDomainType/$catalogueItemId", {
                /*
                Classifiers
                 */
                '/classifiers'(resources: 'classifier', excludes: DEFAULT_EXCLUDES_AND_NO_UPDATE)

                /*
                Metadata
                */
                '/metadata'(resources: 'metadata', excludes: DEFAULT_EXCLUDES)

                /*
                Annotations
                 */
                '/annotations'(resources: 'annotation', excludes: DEFAULT_EXCLUDES_AND_NO_UPDATE) {
                    '/annotations'(resources: 'annotation', excludes: DEFAULT_EXCLUDES_AND_NO_UPDATE)
                }

                /*
                Semantic Links
                 */
                '/semanticLinks'(resources: 'semanticLink', excludes: DEFAULT_EXCLUDES) {
                    put '/confirm'(controller: 'semanticLink', action: 'confirm')
                }

                /*
                Model Imports
                 */
                '/modelImports'(resources: 'modelImport', excludes: DEFAULT_EXCLUDES_AND_NO_UPDATE)

                /*
                Reference Files
                 */
                '/referenceFiles'(resources: 'referenceFile', excludes: DEFAULT_EXCLUDES)

                /*
                Get Catalogue Item by path where is ID of top Catalogue Item is provided
                 */
                get "/path/$path"(controller: 'path', action: 'show')

                /*
                Rules
                */
                '/rules'(resources: 'rule', excludes: DEFAULT_EXCLUDES) {
                    '/representations'(resources: 'ruleRepresentation', excludes: DEFAULT_EXCLUDES)
                }
            }

            /*
            Edits
             */
            get "/$resourceDomainType/$resourceId/edits"(controller: 'edit', action: 'index')

            /*
            Get Catalogue Item by path where is ID of top Catalogue Item is not provided
            */
            get "/$catalogueItemDomainType/path/$path"(controller: 'path', action: 'show')

            /*
            Tree
            */
            group "/tree/$containerDomainType", {
                get '/'(controller: 'treeItem', action: 'index')
                get "/${catalogueItemDomainType}/$catalogueItemId"(controller: 'treeItem', action: 'show')
                get "/search/$searchTerm"(controller: 'treeItem', action: 'search')
            }

            /*
            Metadata
             */
            get "/metadata/namespaces/$id?"(controller: 'metadata', action: 'namespaces')

            /*
            Version Links
             */
            "/$modelDomainType/$modelId/versionLinks"(resources: 'versionLink', excludes: DEFAULT_EXCLUDES)

            /*
            User Images
             */
            get "/userImageFiles/$id"(controller: 'userImageFile', action: 'show')

            /*
            ATOM feed
            */
            get "/feeds/all"(controller: 'feed', action: 'index')
        }
    }
}
