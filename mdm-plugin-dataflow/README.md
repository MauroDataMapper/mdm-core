# mdm-plugin-datamodel

## Url Mappings Report

See the folllowing for the base list of URLs.
* `mdm-core/README.md`
* `mdm-plugin-datamodel/README.md`

This is a list of the additional URLs available.

```
Controller: dataClassComponent
 |   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents                                                                                                       | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents                                                                                                       | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}                                                                                                 | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}                                                                                                 | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id}                                                                                                 | Action: show
 |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/${type}/${dataClassId}                                                        | Action: alterDataClasses
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/${type}/${dataClassId}                                                        | Action: alterDataClasses

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
 |   POST   | /api/dataFlows/import/${importerNamespace}/${importerName}/${importerVersion}                                                                                                    | Action: importDataFlows
 |   POST   | /api/dataFlows/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                                                                    | Action: exportDataFlows
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/diagramLayout                                                                                                             | Action: updateDiagramLayout
 |   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/import/${importerNamespace}/${importerName}/${importerVersion}                                                            | Action: importDataFlow
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}                                                            | Action: exportDataFlow
 |   POST   | /api/dataModels/${dataModelId}/dataFlows                                                                                                                                         | Action: save
 |   GET    | /api/dataModels/${dataModelId}/dataFlows                                                                                                                                         | Action: index
 |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${id}                                                                                                                                   | Action: delete
 |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${id}                                                                                                                                   | Action: update
 |   GET    | /api/dataModels/${dataModelId}/dataFlows/${id}                                                                                                                                   | Action: show
```