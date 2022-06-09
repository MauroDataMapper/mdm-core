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
package uk.ac.ox.softeng.maurodatamapper.test.functional.merge

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * @since 03/08/2021
 */
@Slf4j
class DataModelPluginMergeBuilder extends BaseTestMergeBuilder {

    DataModelPluginMergeBuilder(BaseFunctionalSpec functionalSpec) {
        super(functionalSpec)
    }

    @Override
    TestMergeData buildComplexModelsForMerging(String folderId) {
        buildComplexModelsForMerging(folderId, null)
    }

    //    Checking method used to verify the imported elements all correctly existed inside the owning DC.
    //    This was done to aid in the branching issue, this code could be deleted
    //    @Transactional
    //    void checkImporting(String dataClassId, int expectedImportedSize, int expectedImportableSize, int expectedImportableAddSize,
    //                        int expectedImportableRemoveSize) {
    //        def importingDc = dataClassService.get(dataClassId)
    //        assert importingDc
    //        assert importingDc.importedDataClasses.size() == expectedImportedSize
    //        List imported = dataClassService.findAllByImportingDataClassId(Utils.toUuid(dataClassId))
    //        assert imported.size() == expectedImportedSize
    //        imported.each {
    //            log.warn 'checking DC being imported {} by {}', it.path, importingDc.path
    //            assert it.importingDataClasses.any { it.path == importingDc.path }
    //        }
    //        assert imported.find { it.label.endsWith('Importable') }.importingDataClasses.size() == expectedImportableSize
    //        if (expectedImportableAddSize) {
    //            assert imported.find { it.label.endsWith('Add') }
    //            assert imported.find { it.label.endsWith('Add') }.importingDataClasses.size() == expectedImportableAddSize
    //        } else assert !imported.find { it.label.endsWith('Add') }
    //        if (expectedImportableRemoveSize) {
    //            assert imported.find { it.label.endsWith('Remove') }
    //            assert imported.find { it.label.endsWith('Remove') }.importingDataClasses.size() == expectedImportableRemoveSize
    //        } else assert !imported.find { it.label.endsWith('Remove') }
    //    }

    TestMergeData buildComplexModelsForMerging(String folderId, String terminologyId) {
        String ca = buildCommonAncestorDataModel(folderId, '1', terminologyId)

        Map basicImportData = buildImportableDataModel(folderId, true)
        addImportableElementsToDataModel(ca, basicImportData)
        Map removeImportData = buildImportableDataModel(folderId, true, 'Remove')
        addImportableElementsToDataModel(ca, removeImportData)

        GET("dataModels/$ca/path/${Utils.safeUrlEncode('dc:existingClass')}")
        verifyResponse OK, response
        //        String dataClassId = responseBody().id
        //        checkImporting(dataClassId, 2, 1, 0, 1)

        PUT("dataModels/$ca/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("dataModels/$ca/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("dataModels/$ca/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        //        String sourceDc = getIdFromPath(source, "dm:Functional Test DataModel 1\$source|dc:existingClass")
        //        String targetDc = getIdFromPath(source, "dm:Functional Test DataModel 1\$main|dc:existingClass")
        //        checkImporting(dataClassId, 2, 3, 0, 3)
        //        checkImporting(sourceDc, 2, 3, 0, 3)
        //        checkImporting(targetDc, 2, 3, 0, 3)

        Map addImportData = buildImportableDataModel(folderId, true, 'Add')
        Map<String, Object> sourceMap = modifySourceDataModel(source, '1', '', null, null,
                                                              addImportData,
                                                              removeImportData)
        //        checkImporting(dataClassId, 2, 3, 0, 2)
        //        checkImporting(sourceDc, 2, 3, 1, 0)
        //        checkImporting(targetDc, 2, 3, 0, 2)

        Map<String, Object> targetMap = modifyTargetDataModel(target)

        new TestMergeData(source: source,
                          target: target,
                          commonAncestor: ca,
                          sourceMap: sourceMap,
                          targetMap: targetMap,
                          otherMap: [importableDataModelIds: [basicImportData.dataModelId, addImportData.dataModelId, removeImportData.dataModelId]]
        )
    }

    /**
     * Build the common ancestor data model.
     * @param folderId
     * @param suffix Appended to DM label
     * @param terminologyId If terminologyId is provided then the built data model will include a Model Data Type pointing to that terminology
     * @return ID of the built Data Model
     */
    String buildCommonAncestorDataModel(String folderId, String suffix = 1, String terminologyId = null) {
        POST("folders/$folderId/dataModels", [
            label: "Functional Test DataModel ${suffix}".toString()
        ])
        verifyResponse(CREATED, response)
        String dataModel1Id = responseBody().id

        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteLeftOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteRightOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyLeftOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyRightOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteAndDelete'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteAndModify'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndDelete'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndModifyReturningNoDifference'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndModifyReturningDifference'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'existingClass'])
        verifyResponse CREATED, response
        String caExistingClass = responseBody().id
        POST("dataModels/$dataModel1Id/dataClasses/$caExistingClass/dataClasses", [label: 'deleteLeftOnlyFromExistingClass'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses/$caExistingClass/dataClasses", [label: 'deleteRightOnlyFromExistingClass'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataTypes", [label: 'existingDataType1', domainType: 'PrimitiveType'])
        verifyResponse CREATED, response
        String dataType1Id = responseBody().id
        POST("dataModels/$dataModel1Id/dataTypes", [label: 'existingDataType2', domainType: 'PrimitiveType'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses/$caExistingClass/dataElements", [label: 'existingDataElement', dataType: dataType1Id])
        verifyResponse CREATED, response

        if (terminologyId) {
            buildCommonAncestorModelDataTypePointingExternally(dataModel1Id, terminologyId)
        }

        dataModel1Id
    }

    String buildCommonAncestorModelDataType(String dataModelId, String terminologyId) {
        // Create a DataElement on the DataModel, with the DataElement having a ModelDataType
        // pointing to the Terminology

        POST("dataModels/$dataModelId/dataTypes", [
            label                  : 'Functional Test Model Data Type',
            domainType             : 'ModelDataType',
            modelResourceDomainType: 'Terminology',
            modelResourceId        : terminologyId
        ])
        verifyResponse(CREATED, response)
        String modelDataTypeId = responseBody().id

        GET("dataModels/$dataModelId/path/${Utils.safeUrlEncode('dc:existingClass')}")
        verifyResponse OK, response
        assert responseBody().id
        String dataClassId = responseBody().id

        POST("dataModels/$dataModelId/dataClasses/$dataClassId/dataElements", [
            label          : 'Functional Test Data Element with Model Data Type',
            domainType     : 'DataElement',
            dataType       : [id: modelDataTypeId],
            minMultiplicity: 1,
            maxMultiplicity: 1
        ])
        verifyResponse(CREATED, response)
        String dataElementId = responseBody().id

        dataElementId
    }

    String buildCommonAncestorModelDataTypePointingExternally(String dataModelId, String terminologyId) {
        // Create a DataElement on the DataModel, with the DataElement having a ModelDataType
        // pointing to a terminology in an external folder

        POST("dataModels/$dataModelId/dataTypes", [
            label                  : 'Functional Test Model Data Type Pointing Externally',
            domainType             : 'ModelDataType',
            modelResourceDomainType: 'Terminology',
            modelResourceId        : terminologyId
        ])
        verifyResponse(CREATED, response)
        String modelDataTypeId = responseBody().id

        GET("dataModels/$dataModelId/path/${Utils.safeUrlEncode('dc:existingClass')}")
        verifyResponse OK, response
        assert responseBody().id
        String dataClassId = responseBody().id

        POST("dataModels/$dataModelId/dataClasses/$dataClassId/dataElements", [
            label          : 'Functional Test Data Element with Model Data Type Pointing Externally',
            domainType     : 'DataElement',
            dataType       : [id: modelDataTypeId],
            minMultiplicity: 1,
            maxMultiplicity: 1
        ])
        verifyResponse(CREATED, response)
        String dataElementId = responseBody().id

        dataElementId
    }

    Map modifySourceDataModel(String source, String suffix, String pathing, String simpleTerminologyId,
                              String complexTerminologyId, Map addImportData = [:], Map removeImportData = [:]) {
        // Modify Source
        Map sourceMap = [
            dataModelId                         : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source"),
            existingClass                       : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:existingClass"),
            existingDataElement                 : getIdFromPath(source, "${pathing}dm:Functional Test DataModel " +
                                                                        "${suffix}\$source|dc:existingClass|de:existingDataElement"),
            existingDataType2                   : getIdFromPath(source, "${pathing}dm:Functional Test DataModel " +
                                                                        "${suffix}\$source|dt:existingDataType2"),
            deleteLeftOnlyFromExistingClass     :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:existingClass|dc:deleteLeftOnlyFromExistingClass"),
            deleteLeftOnly                      : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:deleteLeftOnly"),
            modifyLeftOnly                      : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:modifyLeftOnly"),
            deleteAndDelete                     :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:deleteAndDelete"),
            deleteAndModify                     :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:deleteAndModify"),
            modifyAndDelete                     :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:modifyAndDelete"),
            modifyAndModifyReturningNoDifference:
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:modifyAndModifyReturningNoDifference"),
            modifyAndModifyReturningDifference  :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:modifyAndModifyReturningDifference"),
            metadataModifyOnSource              :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|md:functional.test.modifyOnSource"),
            metadataDeleteFromSource            :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|md:functional.test.deleteFromSource"),
            metadataModifyAndDelete             :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|md:functional.test.modifyAndDelete"),
            modelDataTypeId                     :
                getIdFromPathNoValidation(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dt:Functional Test Model Data Type"),
            externallyPointingModelDataTypeId   : getIdFromPathNoValidation(source,
                                                                            "${pathing}dm:Functional Test DataModel ${suffix}\$source|dt:Functional" +
                                                                            " Test Model Data Type Pointing Externally")
        ]

        sourceMap.simpleTerminologyId = simpleTerminologyId
        sourceMap.complexTerminologyId = complexTerminologyId

        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.existingClass/dataClasses/$sourceMap.deleteLeftOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteLeftOnly")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteAndModify")
        verifyResponse NO_CONTENT, response

        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndDelete", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndModifyReturningDifference", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.existingClass/dataClasses", [label: 'addLeftToExistingClass'])
        verifyResponse CREATED, response
        sourceMap.addLeftToExistingClass = responseBody().id
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addLeftOnly'])
        verifyResponse CREATED, response
        sourceMap.addLeftOnly = responseBody().id
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addAndAddReturningNoDifference'])
        verifyResponse CREATED, response
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionLeft'])
        verifyResponse CREATED, response
        sourceMap.addAndAddReturningDifference = responseBody().id

        POST("dataModels/$sourceMap.dataModelId/dataTypes", [label: 'addLeftOnly', domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        sourceMap.addLeftOnlyDataType = responseBody().id
        POST("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.existingClass/dataElements", [
            label: 'addLeftOnly', dataType: sourceMap.addLeftOnlyDataType
        ])
        verifyResponse CREATED, response
        sourceMap.addLeftOnlyDataElement = responseBody().id

        PUT("dataModels/$sourceMap.dataModelId", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("dataModels/$sourceMap.dataModelId/metadata", [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'])
        verifyResponse CREATED, response
        PUT("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.metadataModifyOnSource", [value: 'source has modified this'])
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.metadataModifyAndDelete", [value: 'source has modified this also'])
        verifyResponse OK, response
        DELETE("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.metadataDeleteFromSource")
        verifyResponse NO_CONTENT, response

        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.existingClass/dataElements/$sourceMap.existingDataElement",
            [dataType: sourceMap.existingDataType2])
        verifyResponse OK, response

        // Update the model data type that was pointing to the Simple Test Terminology to point to the Complex Test Terminology'
        if (sourceMap.externallyPointingModelDataTypeId && sourceMap.complexTerminologyId) {
            PUT("dataModels/$sourceMap.dataModelId/dataTypes/$sourceMap.externallyPointingModelDataTypeId",
                [modelResourceDomainType: 'Terminology', modelResourceId: sourceMap.complexTerminologyId])
            verifyResponse OK, response
        }

        if (addImportData) addImportableElementsToDataModel(sourceMap.dataModelId, addImportData)
        if (removeImportData) removeImportableElementsFromDataModel(sourceMap.dataModelId, removeImportData)

        sourceMap
    }

    Map modifyTargetDataModel(String target, String suffix = '1', String pathing = '') {
        // Modify Target
        Map targetMap = [
            dataModelId                         : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main"),
            existingClass                       : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:existingClass"),
            deleteRightOnly                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteRightOnly"),
            modifyRightOnly                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyRightOnly"),
            deleteRightOnlyFromExistingClass    :
                getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:existingClass|dc:deleteRightOnlyFromExistingClass"),
            deleteAndDelete                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteAndDelete"),
            deleteAndModify                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteAndModify"),
            modifyAndDelete                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyAndDelete"),
            modifyAndModifyReturningNoDifference:
                getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyAndModifyReturningNoDifference"),
            modifyAndModifyReturningDifference  :
                getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyAndModifyReturningDifference"),
            deleteLeftOnly                      : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteLeftOnly"),
            modifyLeftOnly                      : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyLeftOnly"),
            deleteLeftOnlyFromExistingClass     :
                getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:existingClass|dc:deleteLeftOnlyFromExistingClass"),
            metadataModifyAndDelete             :
                getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|md:functional.test.modifyAndDelete"),
        ]

        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.existingClass/dataClasses/$targetMap.deleteRightOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteRightOnly")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndDelete")
        verifyResponse NO_CONTENT, response

        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyRightOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteAndModify", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndModifyReturningDifference", [description: 'DescriptionRight'])
        verifyResponse OK, response

        POST("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.existingClass/dataClasses", [label: 'addRightToExistingClass'])
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addRightOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addAndAddReturningNoDifference'])
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionRight'])
        verifyResponse CREATED, response
        targetMap.addAndAddReturningDifference = responseBody().id

        PUT("dataModels/$targetMap.dataModelId", [description: 'DescriptionRight'])
        verifyResponse OK, response
        DELETE("dataModels/$targetMap.dataModelId/metadata/$targetMap.metadataModifyAndDelete")
        verifyResponse NO_CONTENT, response

        targetMap
    }

    Map buildImportableDataModel(String folderId, boolean finalise, String suffix = '') {
        POST("folders/$folderId/dataModels", [
            label: "Functional Test DataModel Importable ${suffix}".toString()
        ])
        verifyResponse(CREATED, response)
        String dataModelId = responseBody().id

        String fullSuffix = suffix ? " ${suffix}" : ''

        POST("dataModels/$dataModelId/dataTypes", [
            label     : "Functional Test DataType Importable${fullSuffix}".toString(),
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        String dtId = responseBody().id
        POST("dataModels/$dataModelId/dataTypes", [
            label     : "Functional Test DataType Importable 2${fullSuffix}".toString(),
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        String dt2Id = responseBody().id


        POST("dataModels/$dataModelId/dataClasses", [
            label: "Functional Test DataClass Importable${fullSuffix}".toString(),
        ])
        verifyResponse CREATED, response
        String dcId = responseBody().id

        POST("dataModels/$dataModelId/dataClasses", [
            label: "Functional Test DataClass Importable 2${fullSuffix}".toString(),
        ])
        verifyResponse CREATED, response
        String dc2Id = responseBody().id

        POST("dataModels/$dataModelId/dataClasses/$dc2Id/dataElements", [
            label   : "Functional Test DataElement Importable${fullSuffix}".toString(),
            dataType: dt2Id,])
        verifyResponse CREATED, response
        String deId = responseBody().id

        if (finalise) {
            PUT("dataModels/$dataModelId/finalise", [versionChangeType: 'Major'])
            verifyResponse OK, response
        }
        [
            dataModelId               : dataModelId,
            dataClassId               : dcId,
            dataClassWithDataElementId: dc2Id,
            dataElementId             : deId,
            dataTypeId                : dtId,
        ]
    }

    void addImportableElementsToDataModel(String dataModelId, Map importData) {
        GET("dataModels/$dataModelId/path/${Utils.safeUrlEncode('dc:existingClass')}")
        verifyResponse OK, response
        String dataClassId = responseBody().id

        PUT("dataModels/$dataModelId/dataTypes/" +
            "$importData.dataModelId/$importData.dataTypeId", [:])
        verifyResponse OK, response

        PUT("dataModels/$dataModelId/dataClasses/" +
            "$importData.dataModelId/$importData.dataClassId", [:])
        verifyResponse OK, response

        PUT("dataModels/$dataModelId/dataClasses/$dataClassId/dataClasses/" +
            "$importData.dataModelId/$importData.dataClassId", [:])
        verifyResponse OK, response

        PUT("dataModels/$dataModelId/dataClasses/$dataClassId/dataElements/" +
            "$importData.dataModelId/$importData.dataClassWithDataElementId/$importData.dataElementId", [:])
        verifyResponse OK, response
    }

    void removeImportableElementsFromDataModel(String dataModelId, Map importData) {
        GET("dataModels/$dataModelId/path/${Utils.safeUrlEncode('dc:existingClass')}")
        verifyResponse OK, response
        String dataClassId = responseBody().id

        DELETE("dataModels/$dataModelId/dataTypes/" +
               "$importData.dataModelId/$importData.dataTypeId")
        verifyResponse OK, response

        DELETE("dataModels/$dataModelId/dataClasses/" +
               "$importData.dataModelId/$importData.dataClassId")
        verifyResponse OK, response

        DELETE("dataModels/$dataModelId/dataClasses/$dataClassId/dataClasses/" +
               "$importData.dataModelId/$importData.dataClassId")
        verifyResponse OK, response

        DELETE("dataModels/$dataModelId/dataClasses/$dataClassId/dataElements/" +
               "$importData.dataModelId/$importData.dataClassWithDataElementId/$importData.dataElementId")
        verifyResponse OK, response
    }
}
