# Tested Endpoints

The current API endpoints have functional tests present in this testing plugin,
and therefore there they will work as expected.
```
Controller: admin
 |   GET    | /api/admin/status  | Action: status
 |   POST   | /api/admin/editProperties  | Action: editApiProperties
 |   POST   | /api/admin/rebuildLuceneIndexes  | Action: rebuildLuceneIndexes
 |   GET    | /api/admin/properties  | Action: apiProperties

Controller: annotation
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations  | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}  | Action: delete
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}  | Action: show
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations  | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: delete
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: show

Controller: authority
 |   GET    | /api/authorities  | Action: index
 |   GET    | /api/authorities/${id}  | Action: show

Controller: authenticating
 |    *     | /api/authentication/logout  | Action: logout
 |   POST   | /api/authentication/login  | Action: login
 |   POST   | /api/admin/activeSessions  | Action: activeSessionsWithCredentials

Controller: catalogueUser
 |   POST   | /api/admin/catalogueUsers/adminRegister  | Action: adminRegister
 |   GET    | /api/admin/catalogueUsers/pendingCount  | Action: pendingCount
 |   GET    | /api/admin/catalogueUsers/pending  | Action: pending
 |   GET    | /api/admin/catalogueUsers/userExists/${emailAddress}  | Action: userExists
 |   PUT    | /api/admin/catalogueUsers/${catalogueUserId}/rejectRegistration  | Action: rejectRegistration
 |   PUT    | /api/admin/catalogueUsers/${catalogueUserId}/approveRegistration  | Action: approveRegistration
 |   PUT    | /api/admin/catalogueUsers/${catalogueUserId}/adminPasswordReset  | Action: adminPasswordReset
 |   GET    | /api/catalogueUsers/search  | Action: search
 |   POST   | /api/catalogueUsers/search  | Action: search
 |   GET    | /api/catalogueUsers/resetPasswordLink/${emailAddress}  | Action: sendPasswordResetLink
 |   PUT    | /api/catalogueUsers/${catalogueUserId}/resetPassword  | Action: resetPassword
 |   PUT    | /api/catalogueUsers/${catalogueUserId}/changePassword  | Action: changePassword
 |   PUT    | /api/catalogueUsers/${catalogueUserId}/userPreferences  | Action: updateUserPreferences
 |   GET    | /api/catalogueUsers/${catalogueUserId}/userPreferences  | Action: userPreferences
 |   GET    | /api/userGroups/${userGroupId}/catalogueUsers  | Action: index
 |   POST   | /api/catalogueUsers  | Action: save
 |   GET    | /api/catalogueUsers  | Action: index
 |  DELETE  | /api/catalogueUsers/${id}  | Action: delete
 |   PUT    | /api/catalogueUsers/${id}  | Action: update
 |   GET    | /api/catalogueUsers/${id}  | Action: show
 |   GET    | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers  | Action: index

Controller: classifier
 |   POST   | /api/classifiers/${classifierId}/classifiers  | Action: save
 |   GET    | /api/classifiers/${classifierId}/classifiers  | Action: index
 |  DELETE  | /api/classifiers/${classifierId}/readByAuthenticated  | Action: readByAuthenticated
 |   PUT    | /api/classifiers/${classifierId}/readByAuthenticated  | Action: readByAuthenticated
 |  DELETE  | /api/classifiers/${classifierId}/readByEveryone  | Action: readByEveryone
 |   PUT    | /api/classifiers/${classifierId}/readByEveryone  | Action: readByEveryone
 |  DELETE  | /api/classifiers/${classifierId}/classifiers/${id}  | Action: delete
 |   PUT    | /api/classifiers/${classifierId}/classifiers/${id}  | Action: update
 |   GET    | /api/classifiers/${classifierId}/classifiers/${id}  | Action: show
 |   POST   | /api/classifiers  | Action: save
 |   GET    | /api/classifiers  | Action: index
 |  DELETE  | /api/classifiers/${id}  | Action: delete
 |   PUT    | /api/classifiers/${id}  | Action: update
 |   GET    | /api/classifiers/${id}  | Action: show

Controller: codeSet
 |   GET    | /api/codeSets/providers/importers  | Action: importerProviders
 |   GET    | /api/codeSets/providers/exporters  | Action: exporterProviders
 |  DELETE  | /api/codeSets/${codeSetId}/readByAuthenticated  | Action: readByAuthenticated
 |   PUT    | /api/codeSets/${codeSetId}/readByAuthenticated  | Action: readByAuthenticated
 |  DELETE  | /api/codeSets/${codeSetId}/readByEveryone  | Action: readByEveryone
 |   PUT    | /api/codeSets/${codeSetId}/readByEveryone  | Action: readByEveryone
 |   GET    | /api/codeSets/${codeSetId}/availableBranches  | Action: availableBranches
 |   GET    | /api/codeSets/${codeSetId}/currentMainBranch  | Action: currentMainBranch
 |   GET    | /api/codeSets/${codeSetId}/latestVersion  | Action: latestVersion
 |   PUT    | /api/codeSets/${codeSetId}/newForkModel  | Action: newForkModel
 |   PUT    | /api/codeSets/${codeSetId}/newDocumentationVersion  | Action: newDocumentationVersion
  |   PUT    | /api/codeSets/${codeSetId}/newBranchModelVersion  | Action: newBranchModelVersion
 |   PUT    | /api/codeSets/${codeSetId}/finalise  | Action: finalise
 |   POST   | /api/folders/${folderId}/codeSets  | Action: save
 |   GET    | /api/folders/${folderId}/codeSets  | Action: index
 |   PUT    | /api/codeSets/${codeSetId}/folder/${folderId}  | Action: changeFolder
 |   GET    | /api/codeSets/${codeSetId}/diff/${otherModelId}  | Action: diff
 |   GET    | /api/codeSets/${codeSetId}/mergeDiff/${otherModelId}  | Action: mergeDiff
 |   GET    | /api/codeSets/${codeSetId}/commonAncestor/${otherModelId}  | Action: commonAncestor
 |   PUT    | /api/folders/${folderId}/codeSets/${codeSetId}  | Action: changeFolder
 |   GET    | /api/codeSets  | Action: index
 |  DELETE  | /api/codeSets/${id}  | Action: delete
 |   PUT    | /api/codeSets/${id}  | Action: update
 |   GET    | /api/codeSets/${id}  | Action: show

Controller: dataClass
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses  | Action: index
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search  | Action: search
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search  | Action: search
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: show
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${otherDataModelId}/${otherDataClassId}  | Action: copyDataClass
 |   POST   | /api/dataModels/${dataModelId}/dataClasses  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataClasses  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: show
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId}  | Action: copyDataClass


 Controller: dataClassComponent
 |   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}  | Action: show
 |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/${type}/${dataClassId}  | Action: alterDataClasses
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/${type}/${dataClassId}                                                        | Action: alterDataClasses

Controller: dataElement
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: show
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${otherDataModelId}/${otherDataClassId}/${dataElementId}  | Action: copyDataElement

 Controller: dataElementComponent
 |   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id}  | Action: show
 |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${dataElementComponentId}/${type}/${dataElementId}  | Action: alterDataElements
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${dataElementComponentId}/${type}/${dataElementId} | Action: alterDataElements

 Controller: dataFlow
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/diagramLayout  | Action: updateDiagramLayout
 |   POST   | /api/dataModels/${dataModelId}/dataFlows  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataFlows  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${id}  

Controller: dataModel
 |   GET    | /api/dataModels/providers/defaultDataTypeProviders  | Action: defaultDataTypeProviders
 |   GET    | /api/dataModels/providers/importers  | Action: importerProviders
 |   GET    | /api/dataModels/providers/exporters  | Action: exporterProviders
 |   GET    | /api/dataModels/types  | Action: types
 |   POST   | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}  | Action: importDataModels
 |   POST   | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModels
 |  DELETE  | /api/dataModels/${dataModelId}/readByAuthenticated  | Action: readByAuthenticated
 |   PUT    | /api/dataModels/${dataModelId}/readByAuthenticated  | Action: readByAuthenticated
 |  DELETE  | /api/dataModels/${dataModelId}/readByEveryone  | Action: readByEveryone
 |   PUT    | /api/dataModels/${dataModelId}/readByEveryone  | Action: readByEveryone
 |   GET    | /api/dataModels/${dataModelId}/search  | Action: search
 |   POST   | /api/dataModels/${dataModelId}/search  | Action: search
 |   GET    | /api/dataModels/${dataModelId}/hierarchy  | Action: hierarchy
 |   GET    | /api/dataModels/${dataModelId}/availableBranches  | Action: availableBranches
 |   GET    | /api/dataModels/${dataModelId}/currentMainBranch  | Action: currentMainBranch
 |   GET    | /api/dataModels/${dataModelId}/latestVersion  | Action: latestVersion
 |   PUT    | /api/dataModels/${dataModelId}/newForkModel  | Action: newForkModel
 |   PUT    | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion
 |   PUT    | /api/dataModels/${dataModelId}/newBranchModelVersion  | Action: newBranchModelVersion
 |   PUT    | /api/dataModels/${dataModelId}/finalise  | Action: finalise
 |   POST   | /api/folders/${folderId}/dataModels  | Action: save
 |   PUT    | /api/folders/${folderId}/dataModels/${dataModelId}  | Action: changeFolder
 |   PUT    | /api/dataModels/${dataModelId}/folder/${folderId}  | Action: changeFolder
 |   GET    | /api/dataModels/${dataModelId}/diff/${otherDataModelId}  | Action: diff
 |   GET    | /api/dataModels/${dataModelId}/mergeDiff/${otherModelId}  | Action: mergeDiff
 |   GET    | /api/dataModels/${dataModelId}/commonAncestor/${otherModelId}  | Action: commonAncestor
 |   GET    | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel
 |   GET    | /api/dataModels  | Action: index
 |  DELETE  | /api/dataModels/${id}  | Action: delete
 |   PUT    | /api/dataModels/${id}  | Action: update
 |   GET    | /api/dataModels/${id}  | Action: show

Controller: dataType
 |   POST   | /api/dataModels/${dataModelId}/dataTypes  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataTypes  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: show
 |   POST   | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${dataTypeId}  | Action: copyDataType

Controller: edit
 |   GET    | /api/${resourceDomainType}/${resourceId}/edits  | Action: index

Controller: email
 |   GET    | /api/admin/emails  | Action: index

Controller: enumerationValue
 |   POST   | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues  | Action: index
 |   POST   | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues  | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: show
 |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}  | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}  | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}  | Action: show

Controller: folder
 |   POST   | /api/folders/${folderId}/folders  | Action: save
 |   GET    | /api/folders/${folderId}/folders  | Action: index
 |  DELETE  | /api/folders/${folderId}/readByAuthenticated  | Action: readByAuthenticated
 |   PUT    | /api/folders/${folderId}/readByAuthenticated  | Action: readByAuthenticated
 |  DELETE  | /api/folders/${folderId}/readByEveryone  | Action: readByEveryone
 |   PUT    | /api/folders/${folderId}/readByEveryone  | Action: readByEveryone
 |  DELETE  | /api/folders/${folderId}/folders/${id}  | Action: delete
 |   PUT    | /api/folders/${folderId}/folders/${id}  | Action: update
 |   GET    | /api/folders/${folderId}/folders/${id}  | Action: show
 |   POST   | /api/folders  | Action: save
 |   GET    | /api/folders  | Action: index
 |  DELETE  | /api/folders/${id}  | Action: delete
 |   PUT    | /api/folders/${id}  | Action: update
 |   GET    | /api/folders/${id}  | Action: show

Controller: importer
 |   GET    | /api/importer/parameters/${ns}?/${name}?/${version}?  | Action: parameters

Controller: mauroDataMapperProvider
 |   GET    | /api/admin/modules  | Action: modules

Controller: mauroDataMapperServiceProvider
 |   GET    | /api/admin/providers/exporters  | Action: exporterProviders
 |   GET    | /api/admin/providers/emailers  | Action: emailProviders
 |   GET    | /api/admin/providers/dataLoaders  | Action: dataLoaderProviders
 |   GET    | /api/admin/providers/importers  | Action: importerProviders

Controller: metadata
 |   GET    | /api/metadata/namespaces/${id}?  | Action: namespaces
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata  | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: show

Controller: referenceFile
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles  | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: show

Controller: securableResourceGroupRole
 |  DELETE  | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups/${userGroupId}  | Action: delete
 |   POST   | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups/${userGroupId}  | Action: save

Controller: semanticLink
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks  | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: show

Controller: session
 |   GET    | /api/session/isApplicationAdministration     | Action: isApplicationAdministrationSession
 |   GET    | /api/admin/activeSessions  | Action: activeSessions
 |   GET    | /api/session/isAuthenticated/${sesssionId}?  | Action: isAuthenticatedSession

Controller: summaryMetadata
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata  | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: show

Controller: summaryMetadataReport
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports  | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}  | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}  | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}  | Action: show

Controller: term
 |   POST   | /api/terminologies/${terminologyId}/terms  | Action: save
 |   GET    | /api/terminologies/${terminologyId}/terms  | Action: index
 |  DELETE  | /api/terminologies/${terminologyId}/terms/${id}  | Action: delete
 |   PUT    | /api/terminologies/${terminologyId}/terms/${id}  | Action: update
 |   GET    | /api/terminologies/${terminologyId}/terms/${id}  | Action: show

Controller: termRelationship
|   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships  | Action: index
|   POST   | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships  | Action: save
|   GET    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships  | Action: index
|   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships/${id}  | Action: show
|  DELETE  | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}  | Action: delete
|   PUT    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}  | Action: update
|   GET    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}  | Action: show

Controller: termRelationshipType
 |   POST   | /api/terminologies/${terminologyId}/termRelationshipTypes  | Action: save
 |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes  | Action: index
 |  DELETE  | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}  | Action: delete
 |   PUT    | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}  | Action: update
 |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}  | Action: show

Controller: terminology
 |   GET    | /api/terminologies/providers/importers  | Action: importerProviders
 |   GET    | /api/terminologies/providers/exporters  | Action: exporterProviders
 |  DELETE  | /api/terminologies/${terminologyId}/readByAuthenticated  | Action: readByAuthenticated
 |   PUT    | /api/terminologies/${terminologyId}/readByAuthenticated  | Action: readByAuthenticated
 |  DELETE  | /api/terminologies/${terminologyId}/readByEveryone  | Action: readByEveryone
 |   PUT    | /api/terminologies/${terminologyId}/readByEveryone  | Action: readByEveryone
 |   GET    | /api/terminologies/${terminologyId}/availableBranches  | Action: availableBranches
 |   GET    | /api/terminologies/${terminologyId}/currentMainBranch  | Action: currentMainBranch
 |   GET    | /api/terminologies/${terminologyId}/latestVersion  | Action: latestVersion
 |   PUT    | /api/terminologies/${terminologyId}/newForkModel  | Action: newForkModel
 |   PUT    | /api/terminologies/${terminologyId}/newDocumentationVersion  | Action: newDocumentationVersion
 |   PUT    | /api/terminologies/${terminologyId}/newBranchModelVersion  | Action: newBranchModelVersion
 |   PUT    | /api/terminologies/${terminologyId}/finalise  | Action: finalise
 |   POST   | /api/folders/${folderId}/terminologies  | Action: save
 |   PUT    | /api/terminologies/${terminologyId}/folder/${folderId}  | Action: changeFolder
 |   GET    | /api/terminologies/${terminologyId}/diff/${otherModelId}  | Action: diff
 |   GET    | /api/terminologies/${terminologyId}/mergeDiff/${otherModelId}  | Action: mergeDiff
 |   GET    | /api/terminologies/${terminologyId}/commonAncestor/${otherModelId}  | Action: commonAncestor
 |   PUT    | /api/folders/${folderId}/terminologies/${terminologyId}  | Action: changeFolder
 |   GET    | /api/terminologies  | Action: index
 |  DELETE  | /api/terminologies/${id}  | Action: delete
 |   PUT    | /api/terminologies/${id}  | Action: update
 |   GET    | /api/terminologies/${id}  | Action: show

Controller: treeItem
|   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/deleted  | Action: deletedModels
|   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/modelSuperseded  | Action: modelSupersededModels
|   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/documentationSuperseded  | Action: documentationSupersededModels
|   GET    | /api/tree/${containerDomainType}/search/${searchTerm}  | Action: search
|   GET    | /api/tree/${containerDomainType}  | Action: index
|   GET    | /api/tree/${containerDomainType}/${catalogueItemDomainType}/${catalogueItemId}  | Action: show

Controller: userGroup
 |   GET    | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups  | Action: index
 |  DELETE  | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups/${userGroupId}  | Action: updateApplicationGroupRole
 |   PUT    | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups/${userGroupId}  | Action: updateApplicationGroupRole
 |  DELETE  | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers
 |   PUT    | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers
 |   POST   | /api/userGroups  | Action: save
 |   GET    | /api/userGroups  | Action: index
 |  DELETE  | /api/userGroups/${id}  | Action: delete
 |   PUT    | /api/userGroups/${id}  | Action: update
 |   GET    | /api/userGroups/${id}  | Action: show
 |   GET    | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups  | Action: index
 |  DELETE  | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers
 |   PUT    | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers

Controller: userImageFile
 |  DELETE  | /api/catalogueUsers/${catalogueUserId}/image  | Action: delete
 |   PUT    | /api/catalogueUsers/${catalogueUserId}/image  | Action: update
 |   GET    | /api/catalogueUsers/${catalogueUserId}/image  | Action: show
 |   POST   | /api/catalogueUsers/${catalogueUserId}/image  | Action: save

Controller: versionLink
 |   POST   | /api/${modelDomainType}/${modelId}/versionLinks  | Action: save
 |   GET    | /api/${modelDomainType}/${modelId}/versionLinks  | Action: index
 |  DELETE  | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: delete
 |   PUT    | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: update
 |   GET    | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: show
```
