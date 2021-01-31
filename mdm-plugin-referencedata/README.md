# mdm-plugin-referencedata

## Url Mappings Report

See `mdm-core/README.md` for the base list of URLs.
This is a list of the additional URLs available.

```
Controller: dataClass
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses                                                                | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses                                                                | Action: index
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search                                                                     | Action: search
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search                                                                     | Action: search
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/content                                                                    | Action: content
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}                                                          | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}                                                          | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}                                                          | Action: show
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${otherDataModelId}/${otherDataClassId}                        | Action: copyDataClass
 |   POST   | /api/dataModels/${dataModelId}/dataClasses                                                                                           | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataClasses                                                                                           | Action: index
 |   GET    | /api/dataModels/${dataModelId}/allDataClasses                                                                                        | Action: all
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${id}                                                                                     | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataClasses/${id}                                                                                     | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${id}                                                                                     | Action: show
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId}                                                   | Action: copyDataClass

Controller: referenceDataElement
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${dataElementId}/suggestLinks/${otherDataModelId}             | Action: suggestLinks
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements                                                               | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements                                                               | Action: index
 |   GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/dataElements                                                                  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}                                                         | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}                                                         | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}                                                         | Action: show
 |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${otherDataModelId}/${otherDataClassId}/${dataElementId}      | Action: copyDataElement

Controller: dataModel
 |   GET    | /api/dataModels/providers/defaultDataTypeProviders                                                                                   | Action: defaultDataTypeProviders
 |   GET    | /api/dataModels/providers/importers                                                                                                  | Action: importerProviders
 |   GET    | /api/dataModels/providers/exporters                                                                                                  | Action: exporterProviders
 |   GET    | /api/dataModels/types                                                                                                                | Action: types
 |   POST   | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                                                       | Action: importModels
 |   POST   | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                       | Action: exportModels
 |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/clean                                                                                     | Action: deleteAllUnusedDataClasses
 |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/clean                                                                                       | Action: deleteAllUnusedDataTypes
 |   GET    | /api/folders/${folderId}/dataModels                                                                                                  | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/readByAuthenticated                                                                                   | Action: readByAuthenticated
 |   PUT    | /api/dataModels/${dataModelId}/readByAuthenticated                                                                                   | Action: readByAuthenticated
 |  DELETE  | /api/dataModels/${dataModelId}/readByEveryone                                                                                        | Action: readByEveryone
 |   PUT    | /api/dataModels/${dataModelId}/readByEveryone                                                                                        | Action: readByEveryone
 |   GET    | /api/dataModels/${dataModelId}/search                                                                                                | Action: search
 |   POST   | /api/dataModels/${dataModelId}/search                                                                                                | Action: search
 |   GET    | /api/dataModels/${dataModelId}/hierarchy                                                                                             | Action: hierarchy
 |   PUT    | /api/dataModels/${dataModelId}/newForkModel                                                                                       | Action: newForkModel
 |   PUT    | /api/dataModels/${dataModelId}/newDocumentationVersion                                                                               | Action: newDocumentationVersion
 |   PUT    | /api/dataModels/${dataModelId}/finalise                                                                                              | Action: finalise
 |   POST   | /api/folders/${folderId}/dataModels                                                                                                  | Action: save
 |   PUT    | /api/folders/${folderId}/dataModels/${dataModelId}                                                                                   | Action: changeFolder
 |   PUT    | /api/dataModels/${dataModelId}/folder/${folderId}                                                                                    | Action: changeFolder
 |   GET    | /api/dataModels/${dataModelId}/suggestLinks/${otherModelId}                                                                          | Action: suggestLinks
 |   GET    | /api/dataModels/${dataModelId}/diff/${otherModelId}                                                                                  | Action: diff
 |   GET    | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                        | Action: exportModel
 |   GET    | /api/dataModels                                                                                                                      | Action: index
 |  DELETE  | /api/dataModels                                                                                                                      | Action: deleteAll
 |  DELETE  | /api/dataModels/${id}                                                                                                                | Action: delete
 |   PUT    | /api/dataModels/${id}                                                                                                                | Action: update
 |   GET    | /api/dataModels/${id}                                                                                                                | Action: show

Controller: referenceDataType
 |   POST   | /api/dataModels/${dataModelId}/dataTypes                                                                                             | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataTypes                                                                                             | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${id}                                                                                       | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataTypes/${id}                                                                                       | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataTypes/${id}                                                                                       | Action: show
 |   POST   | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${dataTypeId}                                                           | Action: copyDataType

Controller: referenceEnumerationValue
 |   POST   | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues                                               | Action: save
 |   GET    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues                                               | Action: index
 |   POST   | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues                                                             | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues                                                             | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}                                         | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}                                         | Action: update
 |   GET    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}                                         | Action: show
 |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}                                                       | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}                                                       | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/enumerationValues/${id}                                                       | Action: show

Controller: referenceSummaryMetadata
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata                                                                   | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata                                                                   | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${id}                                                             | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${id}                                                             | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${id}                                                             | Action: show

Controller: referenceSummaryMetadataReport
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${summaryMetadataId}/summaryMetadataReports                       | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${summaryMetadataId}/summaryMetadataReports                       | Action: index
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}                 | Action: delete
 |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}                 | Action: update
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceSummaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}                 | Action: show
```
