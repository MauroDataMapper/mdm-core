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
package uk.ac.ox.softeng.maurodatamapper.core

import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES_AND_NO_UPDATE
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.INCLUDES_INDEX_ONLY

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
                post '/rebuildHibernateSearchIndexes'(controller: 'admin', action: 'rebuildHibernateSearchIndexes') // 5.0.0 New URL
                get '/status'(controller: 'admin', action: 'status')
                get '/activeSessions'(controller: 'session', action: 'activeSessions')
                '/emails'(resources: 'email', includes: INCLUDES_INDEX_ONLY)

                '/properties'(resources: 'apiProperty', excludes: DEFAULT_EXCLUDES)
                post '/properties/apply'(controller: 'apiProperty', action: 'apply')

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
            get "/session/isAuthenticated/$sessionId?"(controller: 'session', action: 'isAuthenticatedSession') // New Url
            get '/session/isApplicationAdministration'(controller: 'session', action: 'isApplicationAdministrationSession') // New Url
            get '/properties'(controller: 'apiProperty', action: 'index') {
                openAccess = true
            }
            get '/path/prefixMappings'(controller: 'path', action: 'listAllPrefixMappings')

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

                put "/folder/$destinationFolderId"(controller: 'folder', action: 'changeFolder')

                get "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'folder', action: 'exportFolder')
            }

            group '/folders', {
                post "/import/$importerNamespace/$importerName/$importerVersion"(controller: 'folder', action: 'importFolder')
            }

            '/versionedFolders'(resources: 'versionedFolder', excludes: DEFAULT_EXCLUDES) {
                '/folders'(resources: 'folder', excludes: DEFAULT_EXCLUDES)

                put '/finalise'(controller: 'versionedFolder', action: 'finalise')
                put '/newBranchModelVersion'(controller: 'versionedFolder', action: 'newBranchModelVersion')
                put '/newDocumentationVersion'(controller: 'versionedFolder', action: 'newDocumentationVersion')
                put '/newForkModel'(controller: 'versionedFolder', action: 'newForkModel')

                put '/readByEveryone'(controller: 'versionedFolder', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'versionedFolder', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'versionedFolder', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'versionedFolder', action: 'readByAuthenticated')

                post '/search'(controller: 'versionedFolder', action: 'search')
                get '/search'(controller: 'versionedFolder', action: 'search')

                get "/commonAncestor/$otherVersionedFolderId"(controller: 'versionedFolder', action: 'commonAncestor')
                get '/latestFinalisedModel'(controller: 'versionedFolder', action: 'latestFinalisedModel')
                get '/latestModelVersion'(controller: 'versionedFolder', action: 'latestModelVersion')
                get '/modelVersionTree'(controller: 'versionedFolder', action: 'modelVersionTree')
                get '/currentMainBranch'(controller: 'versionedFolder', action: 'currentMainBranch')
                get '/availableBranches'(controller: 'versionedFolder', action: 'availableBranches')
                get '/simpleModelVersionTree'(controller: 'versionedFolder', action: 'simpleModelVersionTree')

                get "/mergeDiff/$otherVersionedFolderId"(controller: 'versionedFolder', action: 'mergeDiff')
                put "/mergeInto/$otherVersionedFolderId"(controller: 'versionedFolder', action: 'mergeInto')
                get "/diff/$otherVersionedFolderId"(controller: 'versionedFolder', action: 'diff')
            }

            '/classifiers'(resources: 'classifier', excludes: DEFAULT_EXCLUDES) {
                '/classifiers'(resources: 'classifier', excludes: DEFAULT_EXCLUDES)
                get '/catalogueItems'(controller: 'classifier', action: 'catalogueItems') // New URL
                put '/readByEveryone'(controller: 'classifier', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'classifier', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'classifier', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'classifier', action: 'readByAuthenticated')
            }

            '/authorities'(resources: 'authority', excludes: DEFAULT_EXCLUDES)

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
                Reference Files
                 */
                '/referenceFiles'(resources: 'referenceFile', excludes: DEFAULT_EXCLUDES)

                /*
                Rules
                */
                '/rules'(resources: 'rule', excludes: DEFAULT_EXCLUDES) {
                    '/representations'(resources: 'ruleRepresentation', excludes: DEFAULT_EXCLUDES)
                }

                post '/search'(controller: 'search', action: 'prefixLabelSearch')
            }

            /*
         Container accessible resources
          All new URLs
           */
            group "/$containerDomainType/$containerId", {

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
                Reference Files
                 */
                '/referenceFiles'(resources: 'referenceFile', excludes: DEFAULT_EXCLUDES)

                /*
                Rules
                */
                '/rules'(resources: 'rule', excludes: DEFAULT_EXCLUDES) {
                    '/representations'(resources: 'ruleRepresentation', excludes: DEFAULT_EXCLUDES)
                }
            }

            group "/$resourceDomainType/$resourceId", {
                /*
                Edits
                */
                get '/edits'(controller: 'edit', action: 'index')
            }

            group "/$securableResourceDomainType/$securableResourceId", {
                /*
                Get resource by path where securableResourceId is the parent resource containing the path
                 */
                get "/path/$path"(controller: 'path', action: 'show')
            }
            /*
            Get by path where is ID of top resource is not provided
            */
            get "/$securableResourceDomainType/path/$path"(controller: 'path', action: 'show')

            /*
            Changelogs
             */
            get "/$resourceDomainType/$resourceId/changelogs"(controller: 'changelog', action: 'index')
            post "/$resourceDomainType/$resourceId/changelogs"(controller: 'changelog', action: 'save')

            /*
            Tree
            */
            group '/tree', {
                group "/$containerDomainType", {
                    get '/'(controller: 'treeItem', action: 'index')
                    get "/${containerId}"(controller: 'treeItem', action: 'show')
                    get "/${catalogueItemDomainType}/$catalogueItemId"(controller: 'treeItem', action: 'show')
                    get "/${catalogueItemDomainType}/$catalogueItemId/ancestors"(controller: 'treeItem', action: 'ancestors')
                    get "/search/$searchTerm"(controller: 'treeItem', action: 'search')
                }
                "/full/$modelDomainType/$modelId"(controller: 'treeItem', action: 'fullModelTree')
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
        }
    }
}
