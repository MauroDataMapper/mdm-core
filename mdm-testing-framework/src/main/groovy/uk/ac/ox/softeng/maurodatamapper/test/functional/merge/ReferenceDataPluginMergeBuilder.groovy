/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * @since 03/08/2021
 */
class ReferenceDataPluginMergeBuilder extends BaseTestMergeBuilder {

    ReferenceDataPluginMergeBuilder(BaseFunctionalSpec functionalSpec) {
        super(functionalSpec)
    }

    @Override
    TestMergeData buildComplexModelsForMerging(String folderId) {
        String ca = buildCommonAncestorReferenceDataModel(folderId)

        PUT("referenceDataModels/$ca/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("referenceDataModels/$ca/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("referenceDataModels/$ca/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        Map<String, Object> sourceMap = modifySourceReferenceDataModel(source)
        Map<String, Object> targetMap = modifyTargetReferenceDataModel(target)

        new TestMergeData(source: source,
                          target: target,
                          sourceMap: sourceMap,
                          targetMap: targetMap
        )
    }

    String buildCommonAncestorReferenceDataModel(String folderId, String suffix = 1) {
        POST("folders/$folderId/referenceDataModels", [
            label: "Functional Test ReferenceData ${suffix}".toString()
        ])
        verifyResponse(CREATED, response)
        String referenceDataModel1Id = responseBody().id

        POST("referenceDataModels/$referenceDataModel1Id/referenceDataTypes", [label: 'commonReferenceDataType', domainType: 'ReferencePrimitiveType',])
        verifyResponse CREATED, response
        String referenceDataTypeId = responseBody().id

        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'deleteLeftOnly', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'deleteRightOnly', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'modifyLeftOnly', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'modifyRightOnly', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'deleteAndDelete', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'deleteAndModify', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'modifyAndDelete', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'modifyAndModifyReturningNoDifference', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/referenceDataElements", [label: 'modifyAndModifyReturningDifference', referenceDataType: referenceDataTypeId])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'])
        verifyResponse CREATED, response
        POST("referenceDataModels/$referenceDataModel1Id/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'])
        verifyResponse CREATED, response

        referenceDataModel1Id
    }

    Map modifySourceReferenceDataModel(String source, String suffix = '1', String pathing = '') {
        // Modify Source
        Map sourceMap = [
            referenceDataModelId                : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source"),
            deleteLeftOnly                      : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|rde:deleteLeftOnly"),
            modifyLeftOnly                      : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|rde:modifyLeftOnly"),
            deleteAndDelete                     : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|rde:deleteAndDelete"),
            deleteAndModify                     : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|rde:deleteAndModify"),
            modifyAndDelete                     : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|rde:modifyAndDelete"),
            modifyAndModifyReturningNoDifference:
                getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|rde:modifyAndModifyReturningNoDifference"),
            modifyAndModifyReturningDifference  :
                getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|rde:modifyAndModifyReturningDifference"),
            metadataModifyOnSource              : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|md:functional.test.modifyOnSource"),
            metadataDeleteFromSource            : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|md:functional.test.deleteFromSource"),
            metadataModifyAndDelete             : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|md:functional.test.modifyAndDelete"),
            commonReferenceDataTypeId           : getIdFromPath(source, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$source|rdt:commonReferenceDataType")
        ]

        DELETE("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements/$sourceMap.deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements/$sourceMap.deleteLeftOnly")
        verifyResponse NO_CONTENT, response
        DELETE("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements/$sourceMap.deleteAndModify")
        verifyResponse NO_CONTENT, response

        PUT("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements/$sourceMap.modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements/$sourceMap.modifyAndDelete", [description: 'Description'])
        verifyResponse OK, response
        PUT("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements/$sourceMap.modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements/$sourceMap.modifyAndModifyReturningDifference", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataTypes", [label: 'addLeftOnly', domainType: 'ReferencePrimitiveType',])
        verifyResponse CREATED, response
        sourceMap.addLeftOnlyDataType = responseBody().id

        POST("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements", [label: 'addLeftOnly', referenceDataType: sourceMap.addLeftOnlyDataType])
        verifyResponse CREATED, response
        sourceMap.addLeftOnly = responseBody().id
        POST("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements", [label: 'addAndAddReturningNoDifference', referenceDataType: sourceMap.addLeftOnlyDataType])
        verifyResponse CREATED, response
        POST("referenceDataModels/$sourceMap.referenceDataModelId/referenceDataElements", [label: 'addAndAddReturningDifference', referenceDataType: sourceMap.addLeftOnlyDataType, description: 'DescriptionLeft'])
        verifyResponse CREATED, response
        sourceMap.addAndAddReturningDifference = responseBody().id

        PUT("referenceDataModels/$sourceMap.referenceDataModelId", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("referenceDataModels/$sourceMap.referenceDataModelId/metadata", [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'])
        verifyResponse CREATED, response
        PUT("referenceDataModels/$sourceMap.referenceDataModelId/metadata/$sourceMap.metadataModifyOnSource", [value: 'source has modified this'])
        verifyResponse OK, response
        PUT("referenceDataModels/$sourceMap.referenceDataModelId/metadata/$sourceMap.metadataModifyAndDelete", [value: 'source has modified this also'])
        verifyResponse OK, response
        DELETE("referenceDataModels/$sourceMap.referenceDataModelId/metadata/$sourceMap.metadataDeleteFromSource")
        verifyResponse NO_CONTENT, response

        sourceMap
    }

    Map modifyTargetReferenceDataModel(String target, String suffix = '1', String pathing = '') {
        // Modify Target
        Map targetMap = [
            referenceDataModelId                : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main"),
            deleteRightOnly                     : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:deleteRightOnly"),
            modifyRightOnly                     : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:modifyRightOnly"),
            deleteAndDelete                     : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:deleteAndDelete"),
            deleteAndModify                     : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:deleteAndModify"),
            modifyAndDelete                     : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:modifyAndDelete"),
            modifyAndModifyReturningNoDifference:
                getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:modifyAndModifyReturningNoDifference"),
            modifyAndModifyReturningDifference  : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:modifyAndModifyReturningDifference"),
            deleteLeftOnly                      : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:deleteLeftOnly"),
            modifyLeftOnly                      : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rde:modifyLeftOnly"),
            metadataModifyAndDelete             : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|md:functional.test.modifyAndDelete"),
            commonReferenceDataTypeId           : getIdFromPath(target, "${pathing}rdm:Functional Test ReferenceData ${suffix}\$main|rdt:commonReferenceDataType")
        ]

        DELETE("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements/$targetMap.deleteRightOnly")
        verifyResponse NO_CONTENT, response
        DELETE("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements/$targetMap.deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements/$targetMap.modifyAndDelete")
        verifyResponse NO_CONTENT, response

        PUT("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements/$targetMap.modifyRightOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements/$targetMap.deleteAndModify", [description: 'Description'])
        verifyResponse OK, response
        PUT("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements/$targetMap.modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements/$targetMap.modifyAndModifyReturningDifference", [description: 'DescriptionRight'])
        verifyResponse OK, response

        POST("referenceDataModels/$targetMap.referenceDataModelId/referenceDataTypes", [label: 'addRightOnly', domainType: 'ReferencePrimitiveType',])
        verifyResponse CREATED, response
        targetMap.addRightOnlyDataType = responseBody().id

        POST("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements", [label: 'addRightOnly', referenceDataType: targetMap.addRightOnlyDataType])
        verifyResponse CREATED, response
        POST("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements", [label: 'addAndAddReturningNoDifference', referenceDataType: targetMap.addRightOnlyDataType])
        verifyResponse CREATED, response
        POST("referenceDataModels/$targetMap.referenceDataModelId/referenceDataElements", [label: 'addAndAddReturningDifference', referenceDataType: targetMap.addRightOnlyDataType, description: 'DescriptionRight'])
        verifyResponse CREATED, response
        targetMap.addAndAddReturningDifference = responseBody().id

        PUT("referenceDataModels/$targetMap.referenceDataModelId", [description: 'DescriptionRight'])
        verifyResponse OK, response
        DELETE("referenceDataModels/$targetMap.referenceDataModelId/metadata/$targetMap.metadataModifyAndDelete")
        verifyResponse NO_CONTENT, response

        targetMap
    }
}
