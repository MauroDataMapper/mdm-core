# Untested Endpoints

The current API endpoints have no functional test present in this testing plugin,
and therefore there is no guarantee they will work as expected.

```

Controller: catalogueFile
 |  DELETE  | /api/catalogueUsers/${catalogueUserId}/image  | Action: delete
 |  PUT     | /api/catalogueUsers/${catalogueUserId}/image  | Action: update
 |  GET     | /api/catalogueUsers/${catalogueUserId}/image  | Action: show
 |  POST    | /api/catalogueUsers/${catalogueUserId}/image  | Action: save

Controller: classifier
 |  POST    | /api/features/${catalogueItemId}/classifiers  | Action: save
 |  GET     | /api/features/${catalogueItemId}/classifiers  | Action: index
 |  DELETE  | /api/features/${catalogueItemId}/classifiers/${id}  | Action: delete

 |  GET     | /api/classifiers/${classifierId}/catalogueItems  | Action: catalogueItems

 |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers  | Action: save
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers/${id}  | Action: delete
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers/${id}  | Action: show

Controller: dataClass

 |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search  | Action: search
 |  POST    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search  | Action: search
 |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/content  | Action: content
 |  GET     | /api/dataModels/${dataModelId}/allDataClasses  | Action: all


Controller: dataElement
 |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${dataElementId}/suggestLinks/${otherDataModelId}  | Action: suggestLinks
 |  GET     | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/dataElements  | Action: index


Controller: dataModel


 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/clean  | Action: deleteAllUnusedDataClasses
 |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/clean  | Action: deleteAllUnusedDataTypes
 |  GET     | /api/folders/${folderId}/dataModels  | Action: index
 |  GET     | /api/dataModels/${dataModelId}/search  | Action: search
 |  POST    | /api/dataModels/${dataModelId}/search  | Action: search

 |  GET     | /api/dataModels/${dataModelId}/suggestLinks/${otherDataModelId}  | Action: suggestLinks


 |  DELETE  | /api/dataModels  | Action: deleteAll



Controller: groupRole
 |  POST    | /api/admin/groupRoles  | Action: save
 |  GET     | /api/admin/groupRoles  | Action: index
 |  GET     | /api/admin/applicationGroupRoles  | Action: listApplicationGroupRoles
 |  DELETE  | /api/admin/groupRoles/${id}  | Action: delete
 |  PUT     | /api/admin/groupRoles/${id}  | Action: update
 |  GET     | /api/admin/groupRoles/${id}  | Action: show
 |  GET     | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles  | Action: listGroupRolesAvailableToSecurableResource



Controller: referenceFiles
 |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles  | Action: save
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles  | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: delete
 |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: update
 |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: show

Controller: securableResourceGroupRole
 |  POST    | /api/userGroups/${userGroupId}/securableResourceGroupRoles  | Action: save
 |  GET     | /api/userGroups/${userGroupId}/securableResourceGroupRoles  | Action: index
 |  DELETE  | /api/userGroups/${userGroupId}/securableResourceGroupRoles/${id}  | Action: delete
 |  PUT     | /api/userGroups/${userGroupId}/securableResourceGroupRoles/${id}  | Action: update
 |  GET     | /api/userGroups/${userGroupId}/securableResourceGroupRoles/${id}  | Action: show
 |  POST    | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles  | Action: save
 |  GET     | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles  | Action: index
 |  DELETE  | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles/${id}  | Action: delete
 |  PUT     | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles/${id}  | Action: update
 |  GET     | /api/${securableResourceDomainType}/${securableResourceId}/securableResourceGroupRoles/${id}  | Action: show
 |  GET     | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}  | Action: index

Controller: treeItem
 |  GET     | /api/admin/tree/${containerDomainType}/${modelDomainType}/deleted  | Action: deletedModels
 |  GET     | /api/admin/tree/${containerDomainType}/${modelDomainType}/modelSuperseded  | Action: modelSupersededModels
 |  GET     | /api/admin/tree/${containerDomainType}/${modelDomainType}/documentationSuperseded  | Action: documentationSupersededModels


Controller: userGroup
 |  POST    | /api/${containerDomainType}/${containerId}/userGroups  | Action: save
 |  GET     | /api/${containerDomainType}/${containerId}/userGroups  | Action: index
 |  DELETE  | /api/${containerDomainType}/${containerId}/userGroups/${id}  | Action: delete
 |  PUT     | /api/${containerDomainType}/${containerId}/userGroups/${id}  | Action: update
 |  GET     | /api/${containerDomainType}/${containerId}/userGroups/${id}  | Action: show

Controller: userImageFile
 |  GET     | /api/userImageFiles/${id}  | Action: show
```
