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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES
import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES_AND_NO_SAVE

class UrlMappings {

    static mappings = {

        group '/api', {

            // Allows us to control posting referenceDataModels into folders
            post "/folders/$folderId/referenceDataModels"(controller: 'referenceDataModel', action: 'save') // new URL

            '/referenceDataModels'(resources: 'referenceDataModel', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {

                put '/finalise'(controller: 'referenceDataModel', action: 'finalise')
                put '/newBranchModelVersion'(controller: 'referenceDataModel', action: 'newBranchModelVersion')
                put '/newDocumentationVersion'(controller: 'referenceDataModel', action: 'newDocumentationVersion')
                put '/newForkModel'(controller: 'referenceDataModel', action: 'newForkModel') // new URL

                /*
                Version Control
                 */
                get "/commonAncestor/$otherModelId"(controller: 'referenceDataModel', action: 'commonAncestor')
                get '/latestVersion'(controller: 'referenceDataModel', action: 'latestVersion')
                get "/mergeDiff/$otherModelId"(controller: 'referenceDataModel', action: 'mergeDiff')

                get '/currentMainBranch'(controller: 'referenceDataModel', action: 'currentMainBranch')
                get '/availableBranches'(controller: 'referenceDataModel', action: 'availableBranches')

                get "/diff/$otherModelId"(controller: 'referenceDataModel', action: 'diff')
                get "/suggestLinks/$otherModelId"(controller: 'referenceDataModel', action: 'suggestLinks')
                put "/folder/$folderId"(controller: 'referenceDataModel', action: 'changeFolder')
                get "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'referenceDataModel', action: 'exportModel')

                delete '/dataTypes/clean'(controller: 'referenceDataModel', action: 'deleteAllUnusedDataTypes')

                get '/hierarchy'(controller: 'dataModel', action: 'hierarchy')

                post '/search'(controller: 'referenceDataModel', action: 'search')
                get '/search'(controller: 'referenceDataModel', action: 'search')

                put '/readByEveryone'(controller: 'referenceDataModel', action: 'readByEveryone')
                delete '/readByEveryone'(controller: 'referenceDataModel', action: 'readByEveryone')
                put '/readByAuthenticated'(controller: 'referenceDataModel', action: 'readByAuthenticated')
                delete '/readByAuthenticated'(controller: 'referenceDataModel', action: 'readByAuthenticated')

                /**
                 * ReferenceDataValues
                 */

                '/referenceDataValues'(resources: 'referenceDataValue', excludes: DEFAULT_EXCLUDES)

                /**
                 * DataElements
                 */

                '/referenceDataElements'(resources: 'referenceDataElement', excludes: DEFAULT_EXCLUDES) {
                    get "/suggestLinks/$otherDataModelId"(controller: 'dataElement', action: 'suggestLinks')
                }
                post "/referenceDataElements/$otherReferenceDataModelId/$referenceDataElementId"(controller: 'referenceDataElement', action: 'copyReferenceDataElement')
                post '/search'(controller: 'dataElement', action: 'search')
                get '/search'(controller: 'dataElement', action: 'search')

                /**
                 * DataTypes
                 */
                '/referenceDataTypes'(resources: 'referenceDataType', excludes: DEFAULT_EXCLUDES) {
                    get '/referenceDataElements'(controller: 'referenceDataElement', action: 'index')
                    '/enumerationValues'(resources: 'enumerationValue', excludes: DEFAULT_EXCLUDES)
                }
                post "/referenceDataTypes/$otherReferenceDataModelId/$referenceDataTypeId"(controller: 'referenceDataType', action: 'copyReferenceDataType')
                "/enumerationTypes/${enumerationTypeId}/enumerationValues"(resources: 'enumerationValue', excludes: DEFAULT_EXCLUDES)
            }

            group '/referenceDataModels', {
                delete '/'(controller: 'referenceDataModel', action: 'deleteAll')

                group '/providers', {
                    get '/exporters'(controller: 'referenceDataModel', action: 'exporterProviders') // new url
                    get '/importers'(controller: 'referenceDataModel', action: 'importerProviders') // new url
                    get '/defaultDataTypeProviders'(controller: 'referenceDataModel', action: 'defaultDataTypeProviders') // new url
                }

                post "/export/$exporterNamespace/$exporterName/$exporterVersion"(controller: 'referenceDataModel', action: 'exportModels')
                post "/import/$importerNamespace/$importerName/$importerVersion"(controller: 'referenceDataModel', action: 'importModels')
            }

            group "/folders/$folderId", {
                get '/referenceDataModels'(controller: 'referenceDataModel', action: 'index')
                put "/referenceDataModels/$referenceDataModelId"(controller: 'referenceDataModel', action: 'changeFolder')
            }


        }
    }
}
