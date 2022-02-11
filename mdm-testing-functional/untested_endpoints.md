# Untested Endpoints

The current API endpoints have no functional test present in this testing plugin,
and therefore there is no guarantee they will work as expected.

```
Dynamic Mappings
|    *     | ERROR: 400                                                                                                                                                                       | View:   /badRequest
|    *     | ERROR: 401                                                                                                                                                                       | View:   /unauthorised
|    *     | ERROR: 501                                                                                                                                                                       | View:   /notImplemented
|    *     | ERROR: 410                                                                                                                                                                       | View:   /gone
|    *     | ERROR: 404                                                                                                                                                                       | View:   /notFound
|    *     | ERROR: 500                                                                                                                                                                       | View:   /error

Controller: admin
|   GET    | /api/admin/status                                                                                                                                                                | Action: status
|   POST   | /api/admin/rebuildHibernateSearchIndexes                                                                                                                                         | Action: rebuildHibernateSearchIndexes

Controller: annotation
|   POST   | /api/${containerDomainType}/${containerId}/annotations/${annotationId}/annotations                                                                                               | Action: save
|   GET    | /api/${containerDomainType}/${containerId}/annotations/${annotationId}/annotations                                                                                               | Action: index
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations                                                                                       | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations                                                                                       | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/annotations/${annotationId}/annotations/${id}                                                                                         | Action: delete
|   GET    | /api/${containerDomainType}/${containerId}/annotations/${annotationId}/annotations/${id}                                                                                         | Action: show
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}                                                                                 | Action: delete
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}                                                                                 | Action: show
|   POST   | /api/${containerDomainType}/${containerId}/annotations                                                                                                                           | Action: save
|   GET    | /api/${containerDomainType}/${containerId}/annotations                                                                                                                           | Action: index
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations                                                                                                                   | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations                                                                                                                   | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/annotations/${id}                                                                                                                     | Action: delete
|   GET    | /api/${containerDomainType}/${containerId}/annotations/${id}                                                                                                                     | Action: show
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}                                                                                                             | Action: delete
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}                                                                                                             | Action: show

Controller: apiKey
|   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/enable                                                                                                                | Action: enableApiKey
|   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/disable                                                                                                               | Action: disableApiKey
|   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/refresh/${expiresInDays}                                                                                              | Action: refreshApiKey
|   POST   | /api/catalogueUsers/${catalogueUserId}/apiKeys                                                                                                                                   | Action: save
|   GET    | /api/catalogueUsers/${catalogueUserId}/apiKeys                                                                                                                                   | Action: index
|  DELETE  | /api/catalogueUsers/${catalogueUserId}/apiKeys/${id}                                                                                                                             | Action: delete

Controller: apiProperty
|   POST   | /api/admin/properties/apply                                                                                                                                                      | Action: apply
|   POST   | /api/admin/properties                                                                                                                                                            | Action: save
|   GET    | /api/admin/properties                                                                                                                                                            | Action: index
|  DELETE  | /api/admin/properties/${id}                                                                                                                                                      | Action: delete
|   PUT    | /api/admin/properties/${id}                                                                                                                                                      | Action: update
|   GET    | /api/admin/properties/${id}                                                                                                                                                      | Action: show
|   GET    | /api/properties                                                                                                                                                                  | Action: index

Controller: authenticating
|    *     | /api/authentication/logout                                                                                                                                                       | Action: logout
|   POST   | /api/authentication/login                                                                                                                                                        | Action: login
|   POST   | /api/admin/activeSessions                                                                                                                                                        | Action: activeSessionsWithCredentials

Controller: authority
|   POST   | /api/authorities                                                                                                                                                                 | Action: save
|   GET    | /api/authorities                                                                                                                                                                 | Action: index
|  DELETE  | /api/authorities/${id}                                                                                                                                                           | Action: delete
|   PUT    | /api/authorities/${id}                                                                                                                                                           | Action: update
|   GET    | /api/authorities/${id}                                                                                                                                                           | Action: show

Controller: catalogueUser
|   POST   | /api/admin/catalogueUsers/initialAdminUser                                                                                                                                       | Action: createInitialAdminUser
|   GET    | /api/admin/catalogueUsers/exportUsers                                                                                                                                            | Action: exportUsers
|   POST   | /api/admin/catalogueUsers/adminRegister                                                                                                                                          | Action: adminRegister
|   GET    | /api/admin/catalogueUsers/pendingCount                                                                                                                                           | Action: pendingCount
|   GET    | /api/admin/catalogueUsers/pending                                                                                                                                                | Action: pending
|   GET    | /api/admin/catalogueUsers/userExists/${emailAddress}                                                                                                                             | Action: userExists
|   PUT    | /api/admin/catalogueUsers/${catalogueUserId}/rejectRegistration                                                                                                                  | Action: rejectRegistration
|   PUT    | /api/admin/catalogueUsers/${catalogueUserId}/approveRegistration                                                                                                                 | Action: approveRegistration
|   PUT    | /api/admin/catalogueUsers/${catalogueUserId}/adminPasswordReset                                                                                                                  | Action: adminPasswordReset
|   GET    | /api/catalogueUsers/search                                                                                                                                                       | Action: search
|   POST   | /api/catalogueUsers/search                                                                                                                                                       | Action: search
|   GET    | /api/catalogueUsers/resetPasswordLink/${emailAddress}                                                                                                                            | Action: sendPasswordResetLink
|   PUT    | /api/catalogueUsers/${catalogueUserId}/resetPassword                                                                                                                             | Action: resetPassword
|   PUT    | /api/catalogueUsers/${catalogueUserId}/changePassword                                                                                                                            | Action: changePassword
|   PUT    | /api/catalogueUsers/${catalogueUserId}/userPreferences                                                                                                                           | Action: updateUserPreferences
|   GET    | /api/catalogueUsers/${catalogueUserId}/userPreferences                                                                                                                           | Action: userPreferences
|   GET    | /api/userGroups/${userGroupId}/catalogueUsers                                                                                                                                    | Action: index
|   POST   | /api/catalogueUsers                                                                                                                                                              | Action: save
|   GET    | /api/catalogueUsers                                                                                                                                                              | Action: index
|  DELETE  | /api/catalogueUsers/${id}                                                                                                                                                        | Action: delete
|   PUT    | /api/catalogueUsers/${id}                                                                                                                                                        | Action: update
|   GET    | /api/catalogueUsers/${id}                                                                                                                                                        | Action: show
|   GET    | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers                                                                                              | Action: index

Controller: changelog
|   POST   | /api/${resourceDomainType}/${resourceId}/changelogs                                                                                                                              | Action: save
|   GET    | /api/${resourceDomainType}/${resourceId}/changelogs                                                                                                                              | Action: index

Controller: classifier
|   POST   | /api/classifiers/${classifierId}/classifiers                                                                                                                                     | Action: save
|   GET    | /api/classifiers/${classifierId}/classifiers                                                                                                                                     | Action: index
|  DELETE  | /api/classifiers/${classifierId}/readByAuthenticated                                                                                                                             | Action: readByAuthenticated
|   PUT    | /api/classifiers/${classifierId}/readByAuthenticated                                                                                                                             | Action: readByAuthenticated
|  DELETE  | /api/classifiers/${classifierId}/readByEveryone                                                                                                                                  | Action: readByEveryone
|   PUT    | /api/classifiers/${classifierId}/readByEveryone                                                                                                                                  | Action: readByEveryone
|   GET    | /api/classifiers/${classifierId}/catalogueItems                                                                                                                                  | Action: catalogueItems
|  DELETE  | /api/classifiers/${classifierId}/classifiers/${id}                                                                                                                               | Action: delete
|   PUT    | /api/classifiers/${classifierId}/classifiers/${id}                                                                                                                               | Action: update
|   GET    | /api/classifiers/${classifierId}/classifiers/${id}                                                                                                                               | Action: show
|   POST   | /api/classifiers                                                                                                                                                                 | Action: save
|   GET    | /api/classifiers                                                                                                                                                                 | Action: index
|  DELETE  | /api/classifiers/${id}                                                                                                                                                           | Action: delete
|   PUT    | /api/classifiers/${id}                                                                                                                                                           | Action: update
|   GET    | /api/classifiers/${id}                                                                                                                                                           | Action: show
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers                                                                                                                   | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers                                                                                                                   | Action: index
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers/${id}                                                                                                             | Action: delete
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers/${id}                                                                                                             | Action: show

Controller: codeSet
|   GET    | /api/codeSets/providers/importers                                                                                                                                                | Action: importerProviders
|   GET    | /api/codeSets/providers/exporters                                                                                                                                                | Action: exporterProviders
|   PUT    | /api/admin/codeSets/${id}/undoSoftDelete                                                                                                                                         | Action: undoSoftDelete
|   POST   | /api/codeSets/import/${importerNamespace}/${importerName}/${importerVersion}                                                                                                     | Action: importModels
|   POST   | /api/codeSets/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                                                     | Action: exportModels
|   GET    | /api/terminologies/${terminologyId}/terms/${termId}/codeSets                                                                                                                     | Action: index
|  DELETE  | /api/codeSets/${codeSetId}/readByAuthenticated                                                                                                                                   | Action: readByAuthenticated
|   PUT    | /api/codeSets/${codeSetId}/readByAuthenticated                                                                                                                                   | Action: readByAuthenticated
|  DELETE  | /api/codeSets/${codeSetId}/readByEveryone                                                                                                                                        | Action: readByEveryone
|   PUT    | /api/codeSets/${codeSetId}/readByEveryone                                                                                                                                        | Action: readByEveryone
|   GET    | /api/codeSets/${codeSetId}/availableBranches                                                                                                                                     | Action: availableBranches
|   GET    | /api/codeSets/${codeSetId}/currentMainBranch                                                                                                                                     | Action: currentMainBranch
|   GET    | /api/codeSets/${codeSetId}/simpleModelVersionTree                                                                                                                                | Action: simpleModelVersionTree
|   GET    | /api/codeSets/${codeSetId}/modelVersionTree                                                                                                                                      | Action: modelVersionTree
|   GET    | /api/codeSets/${codeSetId}/latestModelVersion                                                                                                                                    | Action: latestModelVersion
|   GET    | /api/codeSets/${codeSetId}/latestFinalisedModel                                                                                                                                  | Action: latestFinalisedModel
|   PUT    | /api/codeSets/${codeSetId}/newForkModel                                                                                                                                          | Action: newForkModel
|   PUT    | /api/codeSets/${codeSetId}/newDocumentationVersion                                                                                                                               | Action: newDocumentationVersion
|   PUT    | /api/codeSets/${codeSetId}/newBranchModelVersion                                                                                                                                 | Action: newBranchModelVersion
|   PUT    | /api/codeSets/${codeSetId}/finalise                                                                                                                                              | Action: finalise
|   POST   | /api/folders/${folderId}/codeSets                                                                                                                                                | Action: save
|   GET    | /api/folders/${folderId}/codeSets                                                                                                                                                | Action: index
|  DELETE  | /api/codeSets/${codeSetId}/terms/${termId}                                                                                                                                       | Action: alterTerms
|   PUT    | /api/codeSets/${codeSetId}/terms/${termId}                                                                                                                                       | Action: alterTerms
|   PUT    | /api/codeSets/${codeSetId}/folder/${folderId}                                                                                                                                    | Action: changeFolder
|   GET    | /api/codeSets/${codeSetId}/diff/${otherModelId}                                                                                                                                  | Action: diff
|   PUT    | /api/codeSets/${codeSetId}/mergeInto/${otherModelId}                                                                                                                             | Action: mergeInto
|   GET    | /api/codeSets/${codeSetId}/mergeDiff/${otherModelId}                                                                                                                             | Action: mergeDiff
|   GET    | /api/codeSets/${codeSetId}/commonAncestor/${otherModelId}                                                                                                                        | Action: commonAncestor
|   PUT    | /api/folders/${folderId}/codeSets/${codeSetId}                                                                                                                                   | Action: changeFolder
|   GET    | /api/codeSets/${codeSetId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                                        | Action: exportModel
|   GET    | /api/codeSets                                                                                                                                                                    | Action: index
|  DELETE  | /api/codeSets                                                                                                                                                                    | Action: deleteAll
|  DELETE  | /api/codeSets/${id}                                                                                                                                                              | Action: delete
|   PUT    | /api/codeSets/${id}                                                                                                                                                              | Action: update
|   GET    | /api/codeSets/${id}                                                                                                                                                              | Action: show

Controller: dataClass
|   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses                                                                                                            | Action: save
|   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses                                                                                                            | Action: index
|   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search                                                                                                                 | Action: search
|   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search                                                                                                                 | Action: search
|   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/content                                                                                                                | Action: content
|  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}                                                                                                      | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}                                                                                                      | Action: update
|   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}                                                                                                      | Action: show
|  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/extends/${otherDataModelId}/${otherDataClassId}                                                                        | Action: extendDataClass
|   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/extends/${otherDataModelId}/${otherDataClassId}                                                                        | Action: extendDataClass
|  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${otherDataModelId}/${otherDataClassId}                                                                    | Action: importDataClass
|   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${otherDataModelId}/${otherDataClassId}                                                                    | Action: importDataClass
|   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${otherDataModelId}/${otherDataClassId}                                                                    | Action: copyDataClass
|  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${otherDataModelId}/${otherDataClassId}/${otherDataElementId}                                             | Action: importDataElement
|   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${otherDataModelId}/${otherDataClassId}/${otherDataElementId}                                             | Action: importDataElement
|   POST   | /api/dataModels/${dataModelId}/dataClasses                                                                                                                                       | Action: save
|   GET    | /api/dataModels/${dataModelId}/dataClasses                                                                                                                                       | Action: index
|   GET    | /api/dataModels/${dataModelId}/allDataClasses                                                                                                                                    | Action: all
|  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${id}                                                                                                                                 | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/dataClasses/${id}                                                                                                                                 | Action: update
|   GET    | /api/dataModels/${dataModelId}/dataClasses/${id}                                                                                                                                 | Action: show
|   POST   | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId}                                                                                               | Action: copyDataClass

Controller: dataClassComponent
|   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents                                                                                                       | Action: save
|   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents                                                                                                       | Action: index
|  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}                                                                                                 | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}                                                                                                 | Action: update
|   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}                                                                                                 | Action: show
|  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/${type}/${dataClassId}                                                        | Action: alterDataClasses
|   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/${type}/${dataClassId}                                                        | Action: alterDataClasses

Controller: dataElement
|   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${dataElementId}/suggestLinks/${otherDataModelId}                                                         | Action: suggestLinks
|   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements                                                                                                           | Action: save
|   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements                                                                                                           | Action: index
|   GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/dataElements                                                                                                              | Action: index
|  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}                                                                                                     | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}                                                                                                     | Action: update
|   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}                                                                                                     | Action: show
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements/${referenceDataElementId}/suggestLinks/${otherDataModelId}                                                | Action: suggestLinks
|   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${otherDataModelId}/${otherDataClassId}/${dataElementId}                                                  | Action: copyDataElement
|   GET    | /api/referenceDataModels/${referenceDataModelId}/search                                                                                                                          | Action: search
|   POST   | /api/referenceDataModels/${referenceDataModelId}/search                                                                                                                          | Action: search
|   GET    | /api/dataModels/${dataModelId}/dataElements                                                                                                                                      | Action: index

Controller: dataElementComponent
|   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents                                                         | Action: save
|   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents                                                         | Action: index
|  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id}                                                   | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id}                                                   | Action: update
|   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id}                                                   | Action: show
|  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${dataElementComponentId}/${type}/${dataElementId}      | Action: alterDataElements
|   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${dataElementComponentId}/${type}/${dataElementId}      | Action: alterDataElements

Controller: dataFlow
|   GET    | /api/dataFlows/providers/importers                                                                                                                                               | Action: importerProviders
|   GET    | /api/dataFlows/providers/exporters                                                                                                                                               | Action: exporterProviders
|   POST   | /api/dataModels/${dataModelId}/dataFlows/import/${importerNamespace}/${importerName}/${importerVersion}                                                                          | Action: importDataFlows
|   POST   | /api/dataModels/${dataModelId}/dataFlows/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                          | Action: exportDataFlows
|   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/diagramLayout                                                                                                             | Action: updateDiagramLayout
|   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                            | Action: exportDataFlow
|   POST   | /api/dataModels/${dataModelId}/dataFlows                                                                                                                                         | Action: save
|   GET    | /api/dataModels/${dataModelId}/dataFlows                                                                                                                                         | Action: index
|  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${id}                                                                                                                                   | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/dataFlows/${id}                                                                                                                                   | Action: update
|   GET    | /api/dataModels/${dataModelId}/dataFlows/${id}                                                                                                                                   | Action: show

Controller: dataModel
|   GET    | /api/dataModels/providers/defaultDataTypeProviders                                                                                                                               | Action: defaultDataTypeProviders
|   GET    | /api/dataModels/providers/importers                                                                                                                                              | Action: importerProviders
|   GET    | /api/dataModels/providers/exporters                                                                                                                                              | Action: exporterProviders
|   PUT    | /api/admin/dataModels/${id}/undoSoftDelete                                                                                                                                       | Action: undoSoftDelete
|   GET    | /api/dataModels/types                                                                                                                                                            | Action: types
|   POST   | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                                                                                                   | Action: importModels
|   POST   | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                                                   | Action: exportModels
|  DELETE  | /api/dataModels/${dataModelId}/dataClasses/clean                                                                                                                                 | Action: deleteAllUnusedDataClasses
|  DELETE  | /api/dataModels/${dataModelId}/dataTypes/clean                                                                                                                                   | Action: deleteAllUnusedDataTypes
|   GET    | /api/folders/${folderId}/dataModels                                                                                                                                              | Action: index
|  DELETE  | /api/dataModels/${dataModelId}/readByAuthenticated                                                                                                                               | Action: readByAuthenticated
|   PUT    | /api/dataModels/${dataModelId}/readByAuthenticated                                                                                                                               | Action: readByAuthenticated
|  DELETE  | /api/dataModels/${dataModelId}/readByEveryone                                                                                                                                    | Action: readByEveryone
|   PUT    | /api/dataModels/${dataModelId}/readByEveryone                                                                                                                                    | Action: readByEveryone
|   GET    | /api/dataModels/${dataModelId}/search                                                                                                                                            | Action: search
|   POST   | /api/dataModels/${dataModelId}/search                                                                                                                                            | Action: search
|   GET    | /api/dataModels/${dataModelId}/hierarchy                                                                                                                                         | Action: hierarchy
|   GET    | /api/dataModels/${dataModelId}/availableBranches                                                                                                                                 | Action: availableBranches
|   GET    | /api/dataModels/${dataModelId}/currentMainBranch                                                                                                                                 | Action: currentMainBranch
|   GET    | /api/dataModels/${dataModelId}/simpleModelVersionTree                                                                                                                            | Action: simpleModelVersionTree
|   GET    | /api/dataModels/${dataModelId}/modelVersionTree                                                                                                                                  | Action: modelVersionTree
|   GET    | /api/dataModels/${dataModelId}/latestModelVersion                                                                                                                                | Action: latestModelVersion
|   GET    | /api/dataModels/${dataModelId}/latestFinalisedModel                                                                                                                              | Action: latestFinalisedModel
|   PUT    | /api/dataModels/${dataModelId}/newForkModel                                                                                                                                      | Action: newForkModel
|   PUT    | /api/dataModels/${dataModelId}/newDocumentationVersion                                                                                                                           | Action: newDocumentationVersion
|   PUT    | /api/dataModels/${dataModelId}/newBranchModelVersion                                                                                                                             | Action: newBranchModelVersion
|   PUT    | /api/dataModels/${dataModelId}/finalise                                                                                                                                          | Action: finalise
|   POST   | /api/folders/${folderId}/dataModels                                                                                                                                              | Action: save
|   PUT    | /api/folders/${folderId}/dataModels/${dataModelId}                                                                                                                               | Action: changeFolder
|   PUT    | /api/dataModels/${dataModelId}/folder/${folderId}                                                                                                                                | Action: changeFolder
|   GET    | /api/dataModels/${dataModelId}/suggestLinks/${otherDataModelId}                                                                                                                  | Action: suggestLinks
|   GET    | /api/dataModels/${dataModelId}/diff/${otherModelId}                                                                                                                              | Action: diff
|   PUT    | /api/dataModels/${dataModelId}/mergeInto/${otherModelId}                                                                                                                         | Action: mergeInto
|   GET    | /api/dataModels/${dataModelId}/mergeDiff/${otherModelId}                                                                                                                         | Action: mergeDiff
|   GET    | /api/dataModels/${dataModelId}/commonAncestor/${otherModelId}                                                                                                                    | Action: commonAncestor
|  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${otherDataTypeId}                                                                                                  | Action: importDataType
|   PUT    | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${otherDataTypeId}                                                                                                  | Action: importDataType
|  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId}                                                                                               | Action: importDataClass
|   PUT    | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId}                                                                                               | Action: importDataClass
|   GET    | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                                    | Action: exportModel
|   GET    | /api/dataModels                                                                                                                                                                  | Action: index
|  DELETE  | /api/dataModels                                                                                                                                                                  | Action: deleteAll
|  DELETE  | /api/dataModels/${id}                                                                                                                                                            | Action: delete
|   PUT    | /api/dataModels/${id}                                                                                                                                                            | Action: update
|   GET    | /api/dataModels/${id}                                                                                                                                                            | Action: show

Controller: dataType
|   POST   | /api/dataModels/${dataModelId}/dataTypes                                                                                                                                         | Action: save
|   GET    | /api/dataModels/${dataModelId}/dataTypes                                                                                                                                         | Action: index
|  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${id}                                                                                                                                   | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/dataTypes/${id}                                                                                                                                   | Action: update
|   GET    | /api/dataModels/${dataModelId}/dataTypes/${id}                                                                                                                                   | Action: show
|   POST   | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${dataTypeId}                                                                                                       | Action: copyDataType

Controller: edit
|   GET    | /api/${resourceDomainType}/${resourceId}/edits                                                                                                                                   | Action: index

Controller: email
|   GET    | /api/admin/emails                                                                                                                                                                | Action: index

Controller: enumerationValue
|   POST   | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues                                                                                           | Action: save
|   GET    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues                                                                                           | Action: index
|   POST   | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues                                                                                                         | Action: save
|   GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues                                                                                                         | Action: index
|  DELETE  | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}                                                                                     | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}                                                                                     | Action: update
|   GET    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}                                                                                     | Action: show
|  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}                                                                                                   | Action: delete
|   PUT    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}                                                                                                   | Action: update
|   GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}                                                                                                   | Action: show

Controller: feed
|   GET    | /api/feeds/all                                                                                                                                                                   | Action: index

Controller: folder
|   POST   | /api/versionedFolders/${versionedFolderId}/folders                                                                                                                               | Action: save
|   GET    | /api/versionedFolders/${versionedFolderId}/folders                                                                                                                               | Action: index
|   POST   | /api/folders/${folderId}/folders                                                                                                                                                 | Action: save
|   GET    | /api/folders/${folderId}/folders                                                                                                                                                 | Action: index
|   GET    | /api/folders/${folderId}/search                                                                                                                                                  | Action: search
|   POST   | /api/folders/${folderId}/search                                                                                                                                                  | Action: search
|  DELETE  | /api/folders/${folderId}/readByAuthenticated                                                                                                                                     | Action: readByAuthenticated
|   PUT    | /api/folders/${folderId}/readByAuthenticated                                                                                                                                     | Action: readByAuthenticated
|  DELETE  | /api/folders/${folderId}/readByEveryone                                                                                                                                          | Action: readByEveryone
|   PUT    | /api/folders/${folderId}/readByEveryone                                                                                                                                          | Action: readByEveryone
|  DELETE  | /api/versionedFolders/${versionedFolderId}/folders/${id}                                                                                                                         | Action: delete
|   PUT    | /api/versionedFolders/${versionedFolderId}/folders/${id}                                                                                                                         | Action: update
|   GET    | /api/versionedFolders/${versionedFolderId}/folders/${id}                                                                                                                         | Action: show
|  DELETE  | /api/folders/${folderId}/folders/${id}                                                                                                                                           | Action: delete
|   PUT    | /api/folders/${folderId}/folders/${id}                                                                                                                                           | Action: update
|   GET    | /api/folders/${folderId}/folders/${id}                                                                                                                                           | Action: show
|   PUT    | /api/folders/${folderId}/folder/${destinationFolderId}                                                                                                                           | Action: changeFolder
|   POST   | /api/folders                                                                                                                                                                     | Action: save
|   GET    | /api/folders                                                                                                                                                                     | Action: index
|  DELETE  | /api/folders/${id}                                                                                                                                                               | Action: delete
|   PUT    | /api/folders/${id}                                                                                                                                                               | Action: update
|   GET    | /api/folders/${id}                                                                                                                                                               | Action: show

Controller: groupRole
|   POST   | /api/admin/groupRoles                                                                                                                                                            | Action: save
|   GET    | /api/admin/groupRoles                                                                                                                                                            | Action: index
|   GET    | /api/admin/availableApplicationAccess                                                                                                                                            | Action: listApplicationAccess
|   GET    | /api/admin/applicationGroupRoles                                                                                                                                                 | Action: listApplicationGroupRoles
|  DELETE  | /api/admin/groupRoles/${id}                                                                                                                                                      | Action: delete
|   PUT    | /api/admin/groupRoles/${id}                                                                                                                                                      | Action: update
|   GET    | /api/admin/groupRoles/${id}                                                                                                                                                      | Action: show
|   GET    | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles                                                                                                            | Action: listGroupRolesAvailableToSecurableResource

Controller: importer
|   GET    | /api/importer/parameters/${ns}?/${name}?/${version}?                                                                                                                             | Action: parameters

Controller: mauroDataMapperProvider
|   GET    | /api/admin/modules                                                                                                                                                               | Action: modules

Controller: mauroDataMapperServiceProvider
|   GET    | /api/admin/providers/exporters                                                                                                                                                   | Action: exporterProviders
|   GET    | /api/admin/providers/emailers                                                                                                                                                    | Action: emailProviders
|   GET    | /api/admin/providers/dataLoaders                                                                                                                                                 | Action: dataLoaderProviders
|   GET    | /api/admin/providers/importers                                                                                                                                                   | Action: importerProviders

Controller: metadata
|   GET    | /api/metadata/namespaces/${id}?                                                                                                                                                  | Action: namespaces
|   POST   | /api/${containerDomainType}/${containerId}/metadata                                                                                                                              | Action: save
|   GET    | /api/${containerDomainType}/${containerId}/metadata                                                                                                                              | Action: index
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata                                                                                                                      | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata                                                                                                                      | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/metadata/${id}                                                                                                                        | Action: delete
|   PUT    | /api/${containerDomainType}/${containerId}/metadata/${id}                                                                                                                        | Action: update
|   GET    | /api/${containerDomainType}/${containerId}/metadata/${id}                                                                                                                        | Action: show
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}                                                                                                                | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}                                                                                                                | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}                                                                                                                | Action: show

Controller: modelImport
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports                                                                                                                  | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports                                                                                                                  | Action: index
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports/${id}                                                                                                            | Action: delete
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports/${id}                                                                                                            | Action: show

Controller: path
|   GET    | /api/path/prefixMappings                                                                                                                                                         | Action: listAllPrefixMappings
|   GET    | /api/${securableResourceDomainType}/path/${path}                                                                                                                                 | Action: show
|   GET    | /api/${securableResourceDomainType}/${securableResourceId}/path/${path}                                                                                                          | Action: show

Controller: permissions
|   GET    | /api/${securableResourceDomainType}/${securableResourceId}/permissions                                                                                                           | Action: permissions

Controller: profile
|   GET    | /api/profiles/providers/dynamic                                                                                                                                                  | Action: dynamicProfileProviders
|   GET    | /api/profiles/providers                                                                                                                                                          | Action: profileProviders
|   POST   | /api/profiles/${profileNamespace}/${profileName}/search                                                                                                                          | Action: search
|   GET    | /api/profiles/${profileNamespace}/${profileName}/${multiFacetAwareItemDomainType}/values                                                                                         | Action: listValuesInProfile
|   POST   | /api/profiles/${profileNamespace}/${profileName}/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}/validate                                                              | Action: validate
|   GET    | /api/profiles/${profileNamespace}/${profileName}/${multiFacetAwareItemDomainType}                                                                                                | Action: listModelsInProfile
|  DELETE  | /api/profiles/${profileNamespace}/${profileName}/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}                                                                       | Action: delete
|   POST   | /api/profiles/${profileNamespace}/${profileName}/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}                                                                       | Action: save
|   GET    | /api/profiles/${profileNamespace}/${profileName}/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}                                                                       | Action: show
|   POST   | /api/${modelDomainType}/${modelId}/profile/saveMany                                                                                                                              | Action: saveMany
|   POST   | /api/${modelDomainType}/${modelId}/profile/validateMany                                                                                                                          | Action: validateMany
|   POST   | /api/${modelDomainType}/${modelId}/profile/getMany                                                                                                                               | Action: getMany
|   GET    | /api/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}/profiles/nonProfileMetadata                                                                                       | Action: nonProfileMetadata
|   GET    | /api/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}/profiles/otherMetadata                                                                                            | Action: nonProfileMetadata
|   GET    | /api/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}/profiles/unused                                                                                                   | Action: unusedProfiles
|   GET    | /api/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}/profiles/used                                                                                                     | Action: usedProfiles
|   POST   | /api/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}/profile/${profileNamespace}/${profileName}/${profileVersion}?                                                     | Action: save
|  DELETE  | /api/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}/profile/${profileNamespace}/${profileName}/${profileVersion}?                                                     | Action: delete
|   GET    | /api/${multiFacetAwareItemDomainType}/${multiFacetAwareItemId}/profile/${profileNamespace}/${profileName}/${profileVersion}?                                                     | Action: show

Controller: publish
|   GET    | /api/published/models/${publishedModelId}/newerVersions                                                                                                                          | Action: newerVersions
|   GET    | /api/published/models                                                                                                                                                            | Action: index

Controller: referenceDataElement
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${referenceDataTypeId}/referenceDataElements                                                                 | Action: index
|   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements                                                                                                           | Action: save
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements                                                                                                           | Action: index
|  DELETE  | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements/${id}                                                                                                     | Action: delete
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements/${id}                                                                                                     | Action: update
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements/${id}                                                                                                     | Action: show
|   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements/${otherReferenceDataModelId}/${referenceDataElementId}                                                    | Action: copyReferenceDataElement

Controller: referenceDataModel
|   GET    | /api/referenceDataModels/providers/defaultReferenceDataTypeProviders                                                                                                             | Action: defaultReferenceDataTypeProviders
|   GET    | /api/referenceDataModels/providers/importers                                                                                                                                     | Action: importerProviders
|   GET    | /api/referenceDataModels/providers/exporters                                                                                                                                     | Action: exporterProviders
|   PUT    | /api/admin/referenceDataModels/${id}/undoSoftDelete                                                                                                                              | Action: undoSoftDelete
|   POST   | /api/referenceDataModels/import/${importerNamespace}/${importerName}/${importerVersion}                                                                                          | Action: importModels
|   POST   | /api/referenceDataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                                          | Action: exportModels
|  DELETE  | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/clean                                                                                                        | Action: deleteAllUnusedReferenceDataTypes
|   GET    | /api/folders/${folderId}/referenceDataModels                                                                                                                                     | Action: index
|  DELETE  | /api/referenceDataModels/${referenceDataModelId}/readByAuthenticated                                                                                                             | Action: readByAuthenticated
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/readByAuthenticated                                                                                                             | Action: readByAuthenticated
|  DELETE  | /api/referenceDataModels/${referenceDataModelId}/readByEveryone                                                                                                                  | Action: readByEveryone
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/readByEveryone                                                                                                                  | Action: readByEveryone
|   GET    | /api/referenceDataModels/${referenceDataModelId}/search                                                                                                                          | Action: search
|   POST   | /api/referenceDataModels/${referenceDataModelId}/search                                                                                                                          | Action: search
|   GET    | /api/referenceDataModels/${referenceDataModelId}/hierarchy                                                                                                                       | Action: hierarchy
|   GET    | /api/referenceDataModels/${referenceDataModelId}/availableBranches                                                                                                               | Action: availableBranches
|   GET    | /api/referenceDataModels/${referenceDataModelId}/currentMainBranch                                                                                                               | Action: currentMainBranch
|   GET    | /api/referenceDataModels/${referenceDataModelId}/simpleModelVersionTree                                                                                                          | Action: simpleModelVersionTree
|   GET    | /api/referenceDataModels/${referenceDataModelId}/modelVersionTree                                                                                                                | Action: modelVersionTree
|   GET    | /api/referenceDataModels/${referenceDataModelId}/latestModelVersion                                                                                                              | Action: latestModelVersion
|   GET    | /api/referenceDataModels/${referenceDataModelId}/latestFinalisedModel                                                                                                            | Action: latestFinalisedModel
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/newForkModel                                                                                                                    | Action: newForkModel
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/newDocumentationVersion                                                                                                         | Action: newDocumentationVersion
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/newBranchModelVersion                                                                                                           | Action: newBranchModelVersion
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/finalise                                                                                                                        | Action: finalise
|   POST   | /api/folders/${folderId}/referenceDataModels                                                                                                                                     | Action: save
|   PUT    | /api/folders/${folderId}/referenceDataModels/${referenceDataModelId}                                                                                                             | Action: changeFolder
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/folder/${folderId}                                                                                                              | Action: changeFolder
|   GET    | /api/referenceDataModels/${referenceDataModelId}/suggestLinks/${otherModelId}                                                                                                    | Action: suggestLinks
|   GET    | /api/referenceDataModels/${referenceDataModelId}/diff/${otherModelId}                                                                                                            | Action: diff
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/mergeInto/${otherModelId}                                                                                                       | Action: mergeInto
|   GET    | /api/referenceDataModels/${referenceDataModelId}/mergeDiff/${otherModelId}                                                                                                       | Action: mergeDiff
|   GET    | /api/referenceDataModels/${referenceDataModelId}/commonAncestor/${otherModelId}                                                                                                  | Action: commonAncestor
|   GET    | /api/referenceDataModels/${referenceDataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                  | Action: exportModel
|   GET    | /api/referenceDataModels                                                                                                                                                         | Action: index
|  DELETE  | /api/referenceDataModels                                                                                                                                                         | Action: deleteAll
|  DELETE  | /api/referenceDataModels/${id}                                                                                                                                                   | Action: delete
|   PUT    | /api/referenceDataModels/${id}                                                                                                                                                   | Action: update
|   GET    | /api/referenceDataModels/${id}                                                                                                                                                   | Action: show

Controller: referenceDataType
|   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes                                                                                                              | Action: save
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes                                                                                                              | Action: index
|  DELETE  | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${id}                                                                                                        | Action: delete
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${id}                                                                                                        | Action: update
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${id}                                                                                                        | Action: show
|   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${otherReferenceDataModelId}/${referenceDataTypeId}                                                          | Action: copyReferenceDataType

Controller: referenceDataValue
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/search                                                                                                      | Action: search
|   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/search                                                                                                      | Action: search
|   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues                                                                                                             | Action: save
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues                                                                                                             | Action: index
|  DELETE  | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${id}                                                                                                       | Action: delete
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${id}                                                                                                       | Action: update
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${id}                                                                                                       | Action: show

Controller: referenceEnumerationValue
|   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceEnumerationTypes/${referenceEnumerationTypeId}/referenceEnumerationValues                                              | Action: save
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceEnumerationTypes/${referenceEnumerationTypeId}/referenceEnumerationValues                                              | Action: index
|   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${referenceDataTypeId}/referenceEnumerationValues                                                            | Action: save
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${referenceDataTypeId}/referenceEnumerationValues                                                            | Action: index
|  DELETE  | /api/referenceDataModels/${referenceDataModelId}/referenceEnumerationTypes/${referenceEnumerationTypeId}/referenceEnumerationValues/${id}                                        | Action: delete
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceEnumerationTypes/${referenceEnumerationTypeId}/referenceEnumerationValues/${id}                                        | Action: update
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceEnumerationTypes/${referenceEnumerationTypeId}/referenceEnumerationValues/${id}                                        | Action: show
|  DELETE  | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${referenceDataTypeId}/referenceEnumerationValues/${id}                                                      | Action: delete
|   PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${referenceDataTypeId}/referenceEnumerationValues/${id}                                                      | Action: update
|   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${referenceDataTypeId}/referenceEnumerationValues/${id}                                                      | Action: show

Controller: referenceFile
|   POST   | /api/${containerDomainType}/${containerId}/referenceFiles                                                                                                                        | Action: save
|   GET    | /api/${containerDomainType}/${containerId}/referenceFiles                                                                                                                        | Action: index
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles                                                                                                                | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles                                                                                                                | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/referenceFiles/${id}                                                                                                                  | Action: delete
|   PUT    | /api/${containerDomainType}/${containerId}/referenceFiles/${id}                                                                                                                  | Action: update
|   GET    | /api/${containerDomainType}/${containerId}/referenceFiles/${id}                                                                                                                  | Action: show
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}                                                                                                          | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}                                                                                                          | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}                                                                                                          | Action: show

Controller: referenceSummaryMetadata
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata                                                                                                      | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata                                                                                                      | Action: index
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${id}                                                                                                | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${id}                                                                                                | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${id}                                                                                                | Action: show

Controller: referenceSummaryMetadataReport
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${referenceSummaryMetadataId}/summaryMetadataReports                                                 | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${referenceSummaryMetadataId}/summaryMetadataReports                                                 | Action: index
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${referenceSummaryMetadataId}/summaryMetadataReports/${id}                                           | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${referenceSummaryMetadataId}/summaryMetadataReports/${id}                                           | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${referenceSummaryMetadataId}/summaryMetadataReports/${id}                                           | Action: show

Controller: rule
|   POST   | /api/${containerDomainType}/${containerId}/rules                                                                                                                                 | Action: save
|   GET    | /api/${containerDomainType}/${containerId}/rules                                                                                                                                 | Action: index
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/rules                                                                                                                         | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules                                                                                                                         | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/rules/${id}                                                                                                                           | Action: delete
|   PUT    | /api/${containerDomainType}/${containerId}/rules/${id}                                                                                                                           | Action: update
|   GET    | /api/${containerDomainType}/${containerId}/rules/${id}                                                                                                                           | Action: show
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}                                                                                                                   | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}                                                                                                                   | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}                                                                                                                   | Action: show

Controller: ruleRepresentation
|   POST   | /api/${containerDomainType}/${containerId}/rules/${ruleId}/representations                                                                                                       | Action: save
|   GET    | /api/${containerDomainType}/${containerId}/rules/${ruleId}/representations                                                                                                       | Action: index
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations                                                                                               | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations                                                                                               | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/rules/${ruleId}/representations/${id}                                                                                                 | Action: delete
|   PUT    | /api/${containerDomainType}/${containerId}/rules/${ruleId}/representations/${id}                                                                                                 | Action: update
|   GET    | /api/${containerDomainType}/${containerId}/rules/${ruleId}/representations/${id}                                                                                                 | Action: show
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}                                                                                         | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}                                                                                         | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}                                                                                         | Action: show

Controller: search
|   GET    | /api/catalogueItems/search                                                                                                                                                       | Action: search
|   POST   | /api/catalogueItems/search                                                                                                                                                       | Action: search

Controller: securableResourceGroupRole
|   POST   | /api/userGroups/${userGroupId}/securableResourceGroupRoles                                                                                                                       | Action: save
|   GET    | /api/userGroups/${userGroupId}/securableResourceGroupRoles                                                                                                                       | Action: index
|  DELETE  | /api/userGroups/${userGroupId}/securableResourceGroupRoles/${id}                                                                                                                 | Action: delete
|   PUT    | /api/userGroups/${userGroupId}/securableResourceGroupRoles/${id}                                                                                                                 | Action: update
|   GET    | /api/userGroups/${userGroupId}/securableResourceGroupRoles/${id}                                                                                                                 | Action: show
|  DELETE  | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups/${userGroupId}                                                                   | Action: delete
|   POST   | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups/${userGroupId}                                                                   | Action: save
|   POST   | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles                                                                                           | Action: save
|   GET    | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles                                                                                           | Action: index
|  DELETE  | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles/${id}                                                                                     | Action: delete
|   PUT    | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles/${id}                                                                                     | Action: update
|   GET    | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles/${id}                                                                                     | Action: show
|   GET    | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}                                                                                             | Action: index

Controller: semanticLink
|   PUT    | /api/${containerDomainType}/${containerId}/semanticLinks/${semanticLinkId}/confirm                                                                                               | Action: confirm
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${semanticLinkId}/confirm                                                                                       | Action: confirm
|   POST   | /api/${containerDomainType}/${containerId}/semanticLinks                                                                                                                         | Action: save
|   GET    | /api/${containerDomainType}/${containerId}/semanticLinks                                                                                                                         | Action: index
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks                                                                                                                 | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks                                                                                                                 | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/semanticLinks/${id}                                                                                                                   | Action: delete
|   PUT    | /api/${containerDomainType}/${containerId}/semanticLinks/${id}                                                                                                                   | Action: update
|   GET    | /api/${containerDomainType}/${containerId}/semanticLinks/${id}                                                                                                                   | Action: show
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}                                                                                                           | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}                                                                                                           | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}                                                                                                           | Action: show

Controller: session
|   GET    | /api/session/keepAlive                                                                                                                                                           | Action: keepAlive
|   GET    | /api/session/isApplicationAdministration                                                                                                                                         | Action: isApplicationAdministrationSession
|   GET    | /api/admin/activeSessions                                                                                                                                                        | Action: activeSessions
|   GET    | /api/session/isAuthenticated/${sessionId}?                                                                                                                                       | Action: isAuthenticatedSession

Controller: subscribedCatalogue
|   GET    | /api/admin/subscribedCatalogues/create                                                                                                                                           | Action: create
|   GET    | /api/admin/subscribedCatalogues/${id}/edit                                                                                                                                       | Action: edit
|   GET    | /api/admin/subscribedCatalogues/${subscribedCatalogueId}/testConnection                                                                                                          | Action: testConnection
|   POST   | /api/admin/subscribedCatalogues                                                                                                                                                  | Action: save
|   GET    | /api/admin/subscribedCatalogues                                                                                                                                                  | Action: index
|  DELETE  | /api/admin/subscribedCatalogues/${id}                                                                                                                                            | Action: delete
|  PATCH   | /api/admin/subscribedCatalogues/${id}                                                                                                                                            | Action: patch
|   PUT    | /api/admin/subscribedCatalogues/${id}                                                                                                                                            | Action: update
|   GET    | /api/admin/subscribedCatalogues/${id}                                                                                                                                            | Action: show
|   GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${publishedModelId}/newerVersions                                                                             | Action: newerVersions
|   GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/publishedModels                                                                                                               | Action: publishedModels
|   GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/testConnection                                                                                                                | Action: testConnection
|   GET    | /api/subscribedCatalogues                                                                                                                                                        | Action: index
|   GET    | /api/subscribedCatalogues/${subscribedCatalogueId}                                                                                                                               | Action: show

Controller: subscribedModel
|   GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels/${id}/newerVersions                                                                                          | Action: newerVersions
|   POST   | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels                                                                                                              | Action: save
|   GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels                                                                                                              | Action: index
|  DELETE  | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels/${id}                                                                                                        | Action: delete
|   PUT    | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels/${id}                                                                                                        | Action: update
|   GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels/${id}                                                                                                        | Action: show

Controller: summaryMetadata
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata                                                                                                               | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata                                                                                                               | Action: index
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}                                                                                                         | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}                                                                                                         | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}                                                                                                         | Action: show

Controller: summaryMetadataReport
|   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports                                                                   | Action: save
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports                                                                   | Action: index
|  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}                                                             | Action: delete
|   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}                                                             | Action: update
|   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}                                                             | Action: show

Controller: term
|   GET    | /api/terminologies/${terminologyId}/terms/search                                                                                                                                 | Action: search
|   POST   | /api/terminologies/${terminologyId}/terms/search                                                                                                                                 | Action: search
|   GET    | /api/terminologies/${terminologyId}/terms/tree/${termId}?                                                                                                                        | Action: tree
|   POST   | /api/terminologies/${terminologyId}/terms                                                                                                                                        | Action: save
|   GET    | /api/terminologies/${terminologyId}/terms                                                                                                                                        | Action: index
|   GET    | /api/codeSets/${codeSetId}/terms                                                                                                                                                 | Action: index
|  DELETE  | /api/terminologies/${terminologyId}/terms/${id}                                                                                                                                  | Action: delete
|   PUT    | /api/terminologies/${terminologyId}/terms/${id}                                                                                                                                  | Action: update
|   GET    | /api/terminologies/${terminologyId}/terms/${id}                                                                                                                                  | Action: show

Controller: termRelationship
|   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships                                                                            | Action: index
|   POST   | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships                                                                                                            | Action: save
|   GET    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships                                                                                                            | Action: index
|   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships/${id}                                                                      | Action: show
|  DELETE  | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}                                                                                                      | Action: delete
|   PUT    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}                                                                                                      | Action: update
|   GET    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}                                                                                                      | Action: show

Controller: termRelationshipType
|   POST   | /api/terminologies/${terminologyId}/termRelationshipTypes                                                                                                                        | Action: save
|   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes                                                                                                                        | Action: index
|  DELETE  | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}                                                                                                                  | Action: delete
|   PUT    | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}                                                                                                                  | Action: update
|   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}                                                                                                                  | Action: show

Controller: terminology
|   GET    | /api/terminologies/providers/importers                                                                                                                                           | Action: importerProviders
|   GET    | /api/terminologies/providers/exporters                                                                                                                                           | Action: exporterProviders
|   PUT    | /api/admin/terminologies/${id}/undoSoftDelete                                                                                                                                    | Action: undoSoftDelete
|   POST   | /api/terminologies/import/${importerNamespace}/${importerName}/${importerVersion}                                                                                                | Action: importModels
|   POST   | /api/terminologies/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                                                | Action: exportModels
|  DELETE  | /api/terminologies/${terminologyId}/readByAuthenticated                                                                                                                          | Action: readByAuthenticated
|   PUT    | /api/terminologies/${terminologyId}/readByAuthenticated                                                                                                                          | Action: readByAuthenticated
|  DELETE  | /api/terminologies/${terminologyId}/readByEveryone                                                                                                                               | Action: readByEveryone
|   PUT    | /api/terminologies/${terminologyId}/readByEveryone                                                                                                                               | Action: readByEveryone
|   GET    | /api/terminologies/${terminologyId}/availableBranches                                                                                                                            | Action: availableBranches
|   GET    | /api/terminologies/${terminologyId}/currentMainBranch                                                                                                                            | Action: currentMainBranch
|   GET    | /api/terminologies/${terminologyId}/simpleModelVersionTree                                                                                                                       | Action: simpleModelVersionTree
|   GET    | /api/terminologies/${terminologyId}/modelVersionTree                                                                                                                             | Action: modelVersionTree
|   GET    | /api/terminologies/${terminologyId}/latestModelVersion                                                                                                                           | Action: latestModelVersion
|   GET    | /api/terminologies/${terminologyId}/latestFinalisedModel                                                                                                                         | Action: latestFinalisedModel
|   PUT    | /api/terminologies/${terminologyId}/newForkModel                                                                                                                                 | Action: newForkModel
|   PUT    | /api/terminologies/${terminologyId}/newDocumentationVersion                                                                                                                      | Action: newDocumentationVersion
|   PUT    | /api/terminologies/${terminologyId}/newBranchModelVersion                                                                                                                        | Action: newBranchModelVersion
|   PUT    | /api/terminologies/${terminologyId}/finalise                                                                                                                                     | Action: finalise
|   POST   | /api/folders/${folderId}/terminologies                                                                                                                                           | Action: save
|   GET    | /api/folders/${folderId}/terminologies                                                                                                                                           | Action: index
|   PUT    | /api/terminologies/${terminologyId}/folder/${folderId}                                                                                                                           | Action: changeFolder
|   GET    | /api/terminologies/${terminologyId}/diff/${otherModelId}                                                                                                                         | Action: diff
|   PUT    | /api/terminologies/${terminologyId}/mergeInto/${otherModelId}                                                                                                                    | Action: mergeInto
|   GET    | /api/terminologies/${terminologyId}/mergeDiff/${otherModelId}                                                                                                                    | Action: mergeDiff
|   GET    | /api/terminologies/${terminologyId}/commonAncestor/${otherModelId}                                                                                                               | Action: commonAncestor
|   PUT    | /api/folders/${folderId}/terminologies/${terminologyId}                                                                                                                          | Action: changeFolder
|   GET    | /api/terminologies/${terminologyId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                               | Action: exportModel
|   GET    | /api/terminologies                                                                                                                                                               | Action: index
|  DELETE  | /api/terminologies                                                                                                                                                               | Action: deleteAll
|  DELETE  | /api/terminologies/${id}                                                                                                                                                         | Action: delete
|   PUT    | /api/terminologies/${id}                                                                                                                                                         | Action: update
|   GET    | /api/terminologies/${id}                                                                                                                                                         | Action: show

Controller: treeItem
|   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/deleted                                                                                                                | Action: deletedModels
|   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/modelSuperseded                                                                                                        | Action: modelSupersededModels
|   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/documentationSuperseded                                                                                                | Action: documentationSupersededModels
|    *     | /api/tree/full/${modelDomainType}/${modelId}                                                                                                                                     | Action: fullModelTree
|   GET    | /api/tree/${containerDomainType}/search/${searchTerm}                                                                                                                            | Action: search
|   GET    | /api/tree/${containerDomainType}/${catalogueItemDomainType}/${catalogueItemId}/ancestors                                                                                         | Action: ancestors
|   GET    | /api/tree/${containerDomainType}                                                                                                                                                 | Action: index
|   GET    | /api/tree/${containerDomainType}/${containerId}                                                                                                                                  | Action: show
|   GET    | /api/tree/${containerDomainType}/${catalogueItemDomainType}/${catalogueItemId}                                                                                                   | Action: show

Controller: userGroup
|   GET    | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups                                                                                                            | Action: index
|  DELETE  | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups/${userGroupId}                                                                                             | Action: updateApplicationGroupRole
|   PUT    | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups/${userGroupId}                                                                                             | Action: updateApplicationGroupRole
|  DELETE  | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}                                                                                                                 | Action: alterMembers
|   PUT    | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}                                                                                                                 | Action: alterMembers
|   POST   | /api/userGroups                                                                                                                                                                  | Action: save
|   GET    | /api/userGroups                                                                                                                                                                  | Action: index
|  DELETE  | /api/userGroups/${id}                                                                                                                                                            | Action: delete
|   PUT    | /api/userGroups/${id}                                                                                                                                                            | Action: update
|   GET    | /api/userGroups/${id}                                                                                                                                                            | Action: show
|   GET    | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups                                                                                  | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}                                                                           | Action: alterMembers
|   PUT    | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}                                                                           | Action: alterMembers
|   POST   | /api/${containerDomainType}/${containerId}/userGroups                                                                                                                            | Action: save
|   GET    | /api/${containerDomainType}/${containerId}/userGroups                                                                                                                            | Action: index
|  DELETE  | /api/${containerDomainType}/${containerId}/userGroups/${id}                                                                                                                      | Action: delete
|   PUT    | /api/${containerDomainType}/${containerId}/userGroups/${id}                                                                                                                      | Action: update
|   GET    | /api/${containerDomainType}/${containerId}/userGroups/${id}                                                                                                                      | Action: show

Controller: userImageFile
|  DELETE  | /api/catalogueUsers/${catalogueUserId}/image                                                                                                                                     | Action: delete
|   PUT    | /api/catalogueUsers/${catalogueUserId}/image                                                                                                                                     | Action: update
|   GET    | /api/catalogueUsers/${catalogueUserId}/image                                                                                                                                     | Action: show
|   POST   | /api/catalogueUsers/${catalogueUserId}/image                                                                                                                                     | Action: save
|   GET    | /api/userImageFiles/${id}                                                                                                                                                        | Action: show

Controller: versionLink
|   POST   | /api/${modelDomainType}/${modelId}/versionLinks                                                                                                                                  | Action: save
|   GET    | /api/${modelDomainType}/${modelId}/versionLinks                                                                                                                                  | Action: index
|  DELETE  | /api/${modelDomainType}/${modelId}/versionLinks/${id}                                                                                                                            | Action: delete
|   PUT    | /api/${modelDomainType}/${modelId}/versionLinks/${id}                                                                                                                            | Action: update
|   GET    | /api/${modelDomainType}/${modelId}/versionLinks/${id}                                                                                                                            | Action: show

Controller: versionedFolder
|   POST   | /api/folders/${folderId}/versionedFolders                                                                                                                                        | Action: save
|   GET    | /api/folders/${folderId}/versionedFolders                                                                                                                                        | Action: index
|   GET    | /api/versionedFolders/${versionedFolderId}/simpleModelVersionTree                                                                                                                | Action: simpleModelVersionTree
|   GET    | /api/versionedFolders/${versionedFolderId}/availableBranches                                                                                                                     | Action: availableBranches
|   GET    | /api/versionedFolders/${versionedFolderId}/currentMainBranch                                                                                                                     | Action: currentMainBranch
|   GET    | /api/versionedFolders/${versionedFolderId}/modelVersionTree                                                                                                                      | Action: modelVersionTree
|   GET    | /api/versionedFolders/${versionedFolderId}/latestModelVersion                                                                                                                    | Action: latestModelVersion
|   GET    | /api/versionedFolders/${versionedFolderId}/latestFinalisedModel                                                                                                                  | Action: latestFinalisedModel
|   GET    | /api/versionedFolders/${versionedFolderId}/search                                                                                                                                | Action: search
|   POST   | /api/versionedFolders/${versionedFolderId}/search                                                                                                                                | Action: search
|  DELETE  | /api/versionedFolders/${versionedFolderId}/readByAuthenticated                                                                                                                   | Action: readByAuthenticated
|   PUT    | /api/versionedFolders/${versionedFolderId}/readByAuthenticated                                                                                                                   | Action: readByAuthenticated
|  DELETE  | /api/versionedFolders/${versionedFolderId}/readByEveryone                                                                                                                        | Action: readByEveryone
|   PUT    | /api/versionedFolders/${versionedFolderId}/readByEveryone                                                                                                                        | Action: readByEveryone
|   PUT    | /api/versionedFolders/${versionedFolderId}/newForkModel                                                                                                                          | Action: newForkModel
|   PUT    | /api/versionedFolders/${versionedFolderId}/newDocumentationVersion                                                                                                               | Action: newDocumentationVersion
|   PUT    | /api/versionedFolders/${versionedFolderId}/newBranchModelVersion                                                                                                                 | Action: newBranchModelVersion
|   PUT    | /api/versionedFolders/${versionedFolderId}/finalise                                                                                                                              | Action: finalise
|  DELETE  | /api/folders/${folderId}/versionedFolders/${id}                                                                                                                                  | Action: delete
|   PUT    | /api/folders/${folderId}/versionedFolders/${id}                                                                                                                                  | Action: update
|   GET    | /api/folders/${folderId}/versionedFolders/${id}                                                                                                                                  | Action: show
|   GET    | /api/versionedFolders/${versionedFolderId}/diff/${otherVersionedFolderId}                                                                                                        | Action: diff
|   PUT    | /api/versionedFolders/${versionedFolderId}/mergeInto/${otherVersionedFolderId}                                                                                                   | Action: mergeInto
|   GET    | /api/versionedFolders/${versionedFolderId}/mergeDiff/${otherVersionedFolderId}                                                                                                   | Action: mergeDiff
|   GET    | /api/versionedFolders/${versionedFolderId}/commonAncestor/${otherVersionedFolderId}                                                                                              | Action: commonAncestor
|   POST   | /api/versionedFolders                                                                                                                                                            | Action: save
|   GET    | /api/versionedFolders                                                                                                                                                            | Action: index
|  DELETE  | /api/versionedFolders/${id}                                                                                                                                                      | Action: delete
|   PUT    | /api/versionedFolders/${id}                                                                                                                                                      | Action: update
|   GET    | /api/versionedFolders/${id}                                                                                                                                                      | Action: show
```