# Mdm-Core

## Url Mappings Report
```
Dynamic Mappings
 |    *     | ERROR: 400                                                                                            | View:   /badRequest
 |    *     | ERROR: 401                                                                                            | View:   /unauthorised
 |    *     | ERROR: 501                                                                                            | View:   /notImplemented
 |    *     | ERROR: 410                                                                                            | View:   /gone
 |    *     | ERROR: 404                                                                                            | View:   /notFound
 |    *     | ERROR: 500                                                                                            | View:   /error

Controller: admin
 |   GET    | /api/admin/status                                                                                     | Action: status
 |   POST   | /api/admin/editProperties                                                                             | Action: editApiProperties
 |   POST   | /api/admin/rebuildLuceneIndexes                                                                       | Action: rebuildLuceneIndexes
 |   GET    | /api/admin/properties                                                                                 | Action: apiProperties

Controller: annotation
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations            | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations            | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}      | Action: delete
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}      | Action: show
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations                                        | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations                                        | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}                                  | Action: delete
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}                                  | Action: show

Controller: classifier
 |   POST   | /api/classifiers/${classifierId}/classifiers                                                          | Action: save
 |   GET    | /api/classifiers/${classifierId}/classifiers                                                          | Action: index
 |  DELETE  | /api/classifiers/${classifierId}/readByAuthenticated                                                  | Action: readByAuthenticated
 |   PUT    | /api/classifiers/${classifierId}/readByAuthenticated                                                  | Action: readByAuthenticated
 |  DELETE  | /api/classifiers/${classifierId}/readByEveryone                                                       | Action: readByEveryone
 |   PUT    | /api/classifiers/${classifierId}/readByEveryone                                                       | Action: readByEveryone
 |   GET    | /api/classifiers/${classifierId}/catalogueItems                                                       | Action: catalogueItems
 |  DELETE  | /api/classifiers/${classifierId}/classifiers/${id}                                                    | Action: delete
 |   PUT    | /api/classifiers/${classifierId}/classifiers/${id}                                                    | Action: update
 |   GET    | /api/classifiers/${classifierId}/classifiers/${id}                                                    | Action: show
 |   POST   | /api/classifiers                                                                                      | Action: save
 |   GET    | /api/classifiers                                                                                      | Action: index
 |  DELETE  | /api/classifiers/${id}                                                                                | Action: delete
 |   PUT    | /api/classifiers/${id}                                                                                | Action: update
 |   GET    | /api/classifiers/${id}                                                                                | Action: show
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers                                        | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers                                        | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers/${id}                                  | Action: delete
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers/${id}                                  | Action: show

Controller: edit
 |   GET    | /api/${resourceDomainType}/${resourceId}/edits                                                        | Action: index

Controller: email
 |   GET    | /api/admin/emails                                                                                     | Action: index

Controller: folder
 |   POST   | /api/folders/${folderId}/folders                                                                      | Action: save
 |   GET    | /api/folders/${folderId}/folders                                                                      | Action: index
 |   GET    | /api/folders/${folderId}/search                                                                       | Action: search
 |   POST   | /api/folders/${folderId}/search                                                                       | Action: search
 |  DELETE  | /api/folders/${folderId}/readByAuthenticated                                                          | Action: readByAuthenticated
 |   PUT    | /api/folders/${folderId}/readByAuthenticated                                                          | Action: readByAuthenticated
 |  DELETE  | /api/folders/${folderId}/readByEveryone                                                               | Action: readByEveryone
 |   PUT    | /api/folders/${folderId}/readByEveryone                                                               | Action: readByEveryone
 |  DELETE  | /api/folders/${folderId}/folders/${id}                                                                | Action: delete
 |   PUT    | /api/folders/${folderId}/folders/${id}                                                                | Action: update
 |   GET    | /api/folders/${folderId}/folders/${id}                                                                | Action: show
 |   POST   | /api/folders                                                                                          | Action: save
 |   GET    | /api/folders                                                                                          | Action: index
 |  DELETE  | /api/folders/${id}                                                                                    | Action: delete
 |   PUT    | /api/folders/${id}                                                                                    | Action: update
 |   GET    | /api/folders/${id}                                                                                    | Action: show

Controller: importer
 |   GET    | /api/importer/parameters/${ns}?/${name}?/${version}?                                                  | Action: parameters

Controller: mauroDataMapperProvider
 |   GET    | /api/admin/modules                                                                                    | Action: modules

Controller: mauroDataMapperServiceProvider
 |   GET    | /api/admin/providers/exporters                                                                        | Action: exporterProviders
 |   GET    | /api/admin/providers/emailers                                                                         | Action: emailProviders
 |   GET    | /api/admin/providers/dataLoaders                                                                      | Action: dataLoaderProviders
 |   GET    | /api/admin/providers/importers                                                                        | Action: importerProviders

Controller: metadata
 |   GET    | /api/metadata/namespaces/${id}?                                                                       | Action: namespaces
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata                                           | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata                                           | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}                                     | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}                                     | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}                                     | Action: show

Controller: referenceFiles
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles                                     | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles                                     | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}                               | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}                               | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}                               | Action: show

Controller: search
 |   GET    | /api/catalogueItems/search                                                                            | Action: search
 |   POST   | /api/catalogueItems/search                                                                            | Action: search

Controller: semanticLink
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks                                      | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks                                      | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}                                | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}                                | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}                                | Action: show

Controller: session
 |   GET    | /api/session/keepAlive                                                                                | Action: keepAlive
 |   GET    | /api/admin/activeSessions                                                                             | Action: activeSessions
 |   GET    | /api/session/isAuthenticated/${sesssionId}?                                                           | Action: isAuthenticatedSession

Controller: treeItem
 |   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/deleted                                     | Action: deletedModels
 |   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/modelSuperseded                             | Action: modelSupersededModels
 |   GET    | /api/admin/tree/${containerDomainType}/${modelDomainType}/documentationSuperseded                     | Action: documentationSupersededModels
 |   GET    | /api/tree/${containerDomainType}/search/${searchTerm}                                                 | Action: search
 |   GET    | /api/tree/${containerDomainType}                                                                      | Action: index
 |   GET    | /api/tree/${containerDomainType}/${catalogueItemDomainType}/${catalogueItemId}                        | Action: show

Controller: userImageFile
 |   GET    | /api/userImageFiles/${id}                                                                             | Action: show

Controller: versionLink
 |   POST   | /api/${modelDomainType}/${modelId}/versionLinks                                                       | Action: save
 |   GET    | /api/${modelDomainType}/${modelId}/versionLinks                                                       | Action: index
 |  DELETE  | /api/${modelDomainType}/${modelId}/versionLinks/${id}                                                 | Action: delete
 |   PUT    | /api/${modelDomainType}/${modelId}/versionLinks/${id}                                                 | Action: update
 |   GET    | /api/${modelDomainType}/${modelId}/versionLinks/${id}                                                 | Action: show
```
