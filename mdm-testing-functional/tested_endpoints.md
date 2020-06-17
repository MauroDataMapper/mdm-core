# Tested Endpoints

The current API endpoints have functional tests present in this testing plugin,
and therefore there they will work as expected.
```
Controller: admin
 |  GET     | /api/admin/status  | Action: status
 |  POST    | /api/admin/editProperties  | Action: editApiProperties
 |  POST    | /api/admin/rebuildLuceneIndexes  | Action: rebuildLuceneIndexes
 |  GET     | /api/admin/properties  | Action: apiProperties

Controller: annotation
 |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations        | Action: save
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations        | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}  | Action: delete
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}  | Action: show
 |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations        | Action: save
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations        | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: delete
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: show

Controller: authenticating
 |  GET     | /api/authentication/logout  | Action: logout
 |  POST    | /api/authentication/login  | Action: login
 |  POST    | /api/admin/activeSessions  | Action: activeSessionsWithCredentials

Controller: catalogueUser
 |  POST    | /api/admin/catalogueUsers/adminRegister  | Action: adminRegister
 |  GET     | /api/admin/catalogueUsers/pendingCount  | Action: pendingCount
 |  GET     | /api/admin/catalogueUsers/pending  | Action: pending
 |  GET     | /api/admin/catalogueUsers/userExists/${emailAddress}  | Action: userExists
 |  PUT     | /api/admin/catalogueUsers/${catalogueUserId}/rejectRegistration  | Action: rejectRegistration
 |  PUT     | /api/admin/catalogueUsers/${catalogueUserId}/approveRegistration  | Action: approveRegistration
 |  PUT     | /api/admin/catalogueUsers/${catalogueUserId}/adminPasswordReset  | Action: adminPasswordReset
 |  GET     | /api/catalogueUsers/search  | Action: search
 |  POST    | /api/catalogueUsers/search  | Action: search
 |  GET     | /api/catalogueUsers/resetPasswordLink/${emailAddress}  | Action: sendPasswordResetLink
 |  PUT     | /api/catalogueUsers/${catalogueUserId}/resetPassword  | Action: resetPassword
 |  PUT     | /api/catalogueUsers/${catalogueUserId}/changePassword  | Action: changePassword
 |  PUT     | /api/catalogueUsers/${catalogueUserId}/userPreferences  | Action: updateUserPreferences
 |  GET     | /api/catalogueUsers/${catalogueUserId}/userPreferences  | Action: userPreferences
 |  GET     | /api/userGroups/${userGroupId}/catalogueUsers  | Action: index
 |  POST    | /api/catalogueUsers  | Action: save
 |  GET     | /api/catalogueUsers  | Action: index
 |  DELETE  | /api/catalogueUsers/${id}  | Action: delete
 |  PUT     | /api/catalogueUsers/${id}  | Action: update
 |  GET     | /api/catalogueUsers/${id}  | Action: show
 |  GET     | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers  | Action: index

Controller: classifier
 |  POST    | /api/classifiers        | Action: save
 |  GET     | /api/classifiers        | Action: index
 |  DELETE  | /api/classifiers/${id}  | Action: delete
 |  PUT     | /api/classifiers/${id}  | Action: update
 |  GET     | /api/classifiers/${id}  | Action: show
 |  DELETE  | /api/classifiers/${classifierId}/readByAuthenticated  | Action: readByAuthenticated
 |  PUT     | /api/classifiers/${classifierId}/readByAuthenticated  | Action: readByAuthenticated
 |  DELETE  | /api/classifiers/${classifierId}/readByEveryone       | Action: readByEveryone
 |  PUT     | /api/classifiers/${classifierId}/readByEveryone       | Action: readByEveryone
 |  POST    | /api/classifiers/${classifierId}/classifiers  | Action: save
 |  GET     | /api/classifiers/${classifierId}/classifiers  | Action: index
 |  DELETE  | /api/classifiers/${classifierId}/classifiers/${id}  | Action: delete
 |  PUT     | /api/classifiers/${classifierId}/classifiers/${id}  | Action: update
 |  GET     | /api/classifiers/${classifierId}/classifiers/${id}  | Action: show

Controller: dataClass
 |  POST    | /api/dataModels/${dataModelId}/dataClasses        | Action: save
 |  GET     | /api/dataModels/${dataModelId}/dataClasses        | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: delete
 |  PUT     | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: update
 |  GET     | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: show
 |  POST    | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId}  | Action: copyDataClass
 |  POST    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses        | Action: save
 |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses        | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: delete
 |  PUT     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: update
 |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: show
 |  POST    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${otherDataModelId}/${otherDataClassId}  | Action: copyDataClass

Controller: dataElement
 |  POST    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements        | Action: save
 |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements        | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: delete
 |  PUT     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: update
 |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: show
 |  POST    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${otherDataModelId}/${otherDataClassId}/${dataElementId}  | Action: copyDataElement

Controller: dataModel
 |  GET     | /api/dataModels        | Action: index
 |  DELETE  | /api/dataModels/${id}  | Action: delete
 |  PUT     | /api/dataModels/${id}  | Action: update
 |  GET     | /api/dataModels/${id}  | Action: show
 |  POST    | /api/folders/${folderId}/dataModels                   | Action: save
 |  DELETE  | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 |  PUT     | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 |  DELETE  | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 |  PUT     | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 |  GET     | /api/dataModels/types  | Action: types
 |  GET     | /api/dataModels/${dataModelId}/hierarchy  | Action: hierarchy
 |  PUT     | /api/dataModels/${dataModelId}/finalise   | Action: finalise
 |  PUT     | /api/dataModels/${dataModelId}/newModelVersion  | Action: newModelVersion
 |  PUT     | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion
 |  PUT     | /api/folders/${folderId}/dataModels/${dataModelId}  | Action: changeFolder
 |  PUT     | /api/dataModels/${dataModelId}/folder/${folderId}  | Action: changeFolder
 |  GET     | /api/dataModels/providers/defaultDataTypeProviders  | Action: defaultDataTypeProviders
 |  GET     | /api/dataModels/providers/importers  | Action: importerProviders
 |  GET     | /api/dataModels/providers/exporters  | Action: exporterProviders
 |  GET     | /api/dataModels/${dataModelId}/diff/${otherDataModelId}  | Action: diff
 |  POST    | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}  | Action: importDataModels
 |  POST    | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModels
 |  GET     | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel

Controller: dataType
 |  POST    | /api/dataModels/${dataModelId}/dataTypes        | Action: save
 |  GET     | /api/dataModels/${dataModelId}/dataTypes        | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: delete
 |  PUT     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: update
 |  GET     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: show
 |  POST    | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${dataTypeId}  | Action: copyDataType

Controller: edit
 |  GET     | /api/${resourceDomainType}/${resourceId}/edits  | Action: index

Controller: email
 |  GET     | /api/admin/emails  | Action: index

Controller: enumerationValue
 |  POST    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues        | Action: save
 |  GET     | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues        | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: delete
 |  PUT     | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: update
 |  GET     | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: show
 |  POST    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues        | Action: save
 |  GET     | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues        | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}  | Action: delete
 |  PUT     | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}  | Action: update
 |  GET     | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}  | Action: show

Controller: folder
 |  DELETE  | /api/folders/${folderId}/readByAuthenticated  | Action: readByAuthenticated
 |  PUT     | /api/folders/${folderId}/readByAuthenticated  | Action: readByAuthenticated
 |  DELETE  | /api/folders/${folderId}/readByEveryone  | Action: readByEveryone
 |  PUT     | /api/folders/${folderId}/readByEveryone  | Action: readByEveryone
 |  POST    | /api/folders  | Action: save
 |   GET    | /api/folders  | Action: index
 |  DELETE  | /api/folders/${id}  | Action: delete
 |  PUT     | /api/folders/${id}  | Action: update
 |  GET     | /api/folders/${id}  | Action: show
 |  DELETE  | /api/folders/${folderId}/folders/${id}  | Action: delete
 |  PUT     | /api/folders/${folderId}/folders/${id}  | Action: update
 |  GET     | /api/folders/${folderId}/folders/${id}  | Action: show
 |  POST    | /api/folders/${folderId}/folders  | Action: save
 |  GET     | /api/folders/${folderId}/folders  | Action: index

Controller: importer
 |  GET     | /api/importer/parameters/${ns}?/${name}?/${version}?  | Action: parameters

Controller: metadata
 |  GET     | /api/metadata/namespaces/${id}?  | Action: namespaces

Controller: mauroDataMapperProvider
 |  GET     | /api/admin/modules  | Action: modules

Controller: mauroDataMapperServiceProvider
 |  GET     | /api/admin/providers/exporters  | Action: exporterProviders
 |  GET     | /api/admin/providers/emailers  | Action: emailProviders
 |  GET     | /api/admin/providers/dataLoaders  | Action: dataLoaderProviders
 |  GET     | /api/admin/providers/importers  | Action: importerProviders

Controller: securableResourceGroupRole
 |  DELETE  | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups/${userGroupId}  | Action: delete
 |  POST    | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups/${userGroupId}  | Action: save

Controller: semanticLink
 |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks        | Action: save
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks        | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: delete
 |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: update
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: show

Controller: session
 |  GET     | /api/session/isAuthenticated  | Action: isAuthenticatedSession
 |  GET     | /api/admin/activeSessions  | Action: activeSessions

Controller: treeItem
 |  GET     | /api/tree/${containerDomainType}/search/${search}  | Action: search
 |  GET     | /api/tree/${containerDomainType}  | Action: index
 |  GET     | /api/tree/${containerDomainType}/${catalogueItemDomainType}/${catalogueItemId}  |

Controller: userGroup
 |  GET     | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups  | Action: index
 |  DELETE  | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups/${userGroupId}  | Action: updateApplicationGroupRole
 |  PUT     | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups/${userGroupId}  | Action: updateApplicationGroupRole
 |  DELETE  | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers
 |  PUT     | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers
 |  POST    | /api/userGroups  | Action: save
 |  GET     | /api/userGroups  | Action: index
 |  DELETE  | /api/userGroups/${id}  | Action: delete
 |  PUT     | /api/userGroups/${id}  | Action: update
 |  GET     | /api/userGroups/${id}  | Action: show
 |  GET     | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups  | Action: index
 |  DELETE  | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers
 |  PUT     | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers

Controller: versionLink
 |  POST    | /api/${modelDomainType}/${modelId}/versionLinks  | Action: save
 |  GET     | /api/${modelDomainType}/${modelId}/versionLinks  | Action: index
 |  DELETE  | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: delete
 |  PUT     | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: update
 |  GET     | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: show
```
