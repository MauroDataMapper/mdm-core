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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES_AND_NO_SAVE

class UrlMappings {

    static mappings = {

        group '/api', {

            // Allows us to control posting dataModels into folders
            post "/folders/$folderId/dataModels"(controller: 'dataModel', action: 'save') // new URL

            '/dataModels'(resources: 'dataModel', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {

                put '/finalise'(controller: 'dataModel', action: 'finalise')
                put '/newBranchModelVersion'(controller: 'dataModel', action: 'newBranchModelVersion')
                put '/newDocumentationVersion'(controller: 'dataModel', action: 'newDocumentationVersion')
                put '/newForkModel'(controller: 'dataModel', action: 'newForkModel') // new URL

                /*
                Version Control
                 */
                get "/commonAncestor/$otherModelId"(controller: 'dataModel', action: 'commonAncestor')
                get '/latestFinalisedModel'(controller: 'dataModel', action: 'latestFinalisedModel')
                get '/latestModelVersion'(controller: 'dataModel', action: 'latestModelVersion')
                get "/mergeDiff/$otherModelId"(controller: 'dataModel', action: 'mergeDiff')
                put "/mergeInto/$otherModelId"(controller: 'dataModel', action: 'mergeInto')
                get '/modelVersionTree' (controller: 'dataModel', action: 'modelVersionTree')

                get '/currentMainBranch'(controller: 'dataModel', action: 'currentMainBranch')
                get '/availableBranches'(controller: 'dataModel', action: 'availableBranches')

                get "/diff/$otherModelId"(controller: 'dataModel', action: 'diff')
                get "/suggestLinks/$otherModelId"(controller: 'dataModel', action: 'suggestLinks')
                put "/folder/$folderId"(controller: 'dataModel', action: 'changeFolder')
                get "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'dataModel', action: 'exportModel')

                delete '/dataTypes/clean'(controller: 'dataModel', action: 'deleteAllUnusedDataTypes')
                delete '/dataClasses/clean'(controller: 'dataModel', action: 'deleteAllUnusedDataClasses')

                get '/hierarchy'(controller: 'dataModel', action: 'hierarchy')

                post '/search'(controller: 'dataModel', action: 'search')
                get '/search'(controller: 'dataModel', action: 'search')

                put '/readByEveryone'(controller: 'dataModel', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'dataModel', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'dataModel', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'dataModel', action: 'readByAuthenticated')


                /**
                 * DataClasses
                 */
                '/dataClasses'(resources: 'dataClass', excludes: DEFAULT_EXCLUDES) {
                    '/dataClasses'(resources: 'dataClass', excludes: DEFAULT_EXCLUDES)
                    get '/content'(controller: 'dataClass', action: 'content')
                    post "/dataClasses/$otherDataModelId/$otherDataClassId"(controller: 'dataClass', action: 'copyDataClass')

                    post '/search'(controller: 'dataClass', action: 'search')
                    get '/search'(controller: 'dataClass', action: 'search')

                    /**
                     * DataElements
                     */
                    '/dataElements'(resources: 'dataElement', excludes: DEFAULT_EXCLUDES) {
                        get "/suggestLinks/$otherDataModelId"(controller: 'dataElement', action: 'suggestLinks')
                    }
                    post "/dataElements/$otherDataModelId/$otherDataClassId/$dataElementId"(controller: 'dataElement', action: 'copyDataElement')
                }

                post "/dataClasses/$otherDataModelId/$otherDataClassId"(controller: 'dataClass', action: 'copyDataClass')
                get '/allDataClasses'(controller: 'dataClass', action: 'all')

                /**
                 * DataTypes
                 */
                '/dataTypes'(resources: 'dataType', excludes: DEFAULT_EXCLUDES) {
                    get '/dataElements'(controller: 'dataElement', action: 'index')
                    '/enumerationValues'(resources: 'enumerationValue', excludes: DEFAULT_EXCLUDES)
                }
                post "/dataTypes/$otherDataModelId/$dataTypeId"(controller: 'dataType', action: 'copyDataType')
                "/enumerationTypes/${enumerationTypeId}/enumerationValues"(resources: 'enumerationValue', excludes: DEFAULT_EXCLUDES)
            }

            group '/dataModels', {
                get '/types'(controller: 'dataModel', action: 'types')
                delete '/'(controller: 'dataModel', action: 'deleteAll')

                group '/providers', {
                    get '/exporters'(controller: 'dataModel', action: 'exporterProviders') // new url
                    get '/importers'(controller: 'dataModel', action: 'importerProviders') // new url
                    get '/defaultDataTypeProviders'(controller: 'dataModel', action: 'defaultDataTypeProviders') // new url
                }

                post "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'dataModel', action: 'exportModels')
                post "/import/$importerNamespace/$importerName/$importerVersion"(controller: 'dataModel', action: 'importModels')
            }

            group "/folders/$folderId", {
                get '/dataModels'(controller: 'dataModel', action: 'index')
                put "/dataModels/$dataModelId"(controller: 'dataModel', action: 'changeFolder')
            }

            /**
             * Summary metadata
             */
            group "/$catalogueItemDomainType/$catalogueItemId", {
                '/summaryMetadata'(resources: 'summaryMetadata', excludes: DEFAULT_EXCLUDES) {
                    '/summaryMetadataReports'(resources: 'summaryMetadataReport', excludes: DEFAULT_EXCLUDES)
                }
            }
        }
    }
}
