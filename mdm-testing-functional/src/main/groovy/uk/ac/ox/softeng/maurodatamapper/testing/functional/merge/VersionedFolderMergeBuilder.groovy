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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.merge

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.BaseTestMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.DataModelPluginMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.ReferenceDataPluginMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.TerminologyPluginMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.TestMergeData
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import grails.util.BuildSettings
import io.micronaut.http.HttpResponse

import java.nio.file.Files
import java.nio.file.Paths

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

class VersionedFolderMergeBuilder extends BaseTestMergeBuilder {

    DataModelPluginMergeBuilder dataModelPluginMergeBuilder
    ReferenceDataPluginMergeBuilder referenceDataPluginMergeBuilder
    TerminologyPluginMergeBuilder terminologyPluginMergeBuilder

    UserAccessAndPermissionChangingFunctionalSpec fullFunctionalSpec

    VersionedFolderMergeBuilder(UserAccessAndPermissionChangingFunctionalSpec functionalSpec) {
        super(functionalSpec)
        this.fullFunctionalSpec = functionalSpec
        dataModelPluginMergeBuilder = new DataModelPluginMergeBuilder(functionalSpec)
        terminologyPluginMergeBuilder = new TerminologyPluginMergeBuilder(functionalSpec)
        referenceDataPluginMergeBuilder = new ReferenceDataPluginMergeBuilder(functionalSpec)
    }

    Map buildComplexModelsForFinalisation() {
        // Somethings up with the MD, when running properly the diff happily returns the changed MD, but under test it doesnt.
        // The MD exists in the daabase and is returned if using the MD endpoint but when calling folder.metadata the collection is empty.
        // When run-app all the tables are correctly populated and the collection is not empty
        String simpleTerminologyId = getSimpleTerminologyId()

        loginEditor()
        POST('versionedFolders', [
            label: 'Functional Test VersionedFolder Complex'
        ])
        verifyResponse(CREATED, response)
        String commonAncestorId = responseBody().id

        String dataModelCa = dataModelPluginMergeBuilder.buildCommonAncestorDataModel(commonAncestorId, '1', simpleTerminologyId)
        String terminologyCa = terminologyPluginMergeBuilder.buildCommonAncestorTerminology(commonAncestorId)
        String codeSetCa = terminologyPluginMergeBuilder.buildCommonAncestorCodeSet(commonAncestorId, terminologyCa)
        dataModelPluginMergeBuilder.buildCommonAncestorModelDataType(dataModelCa, terminologyCa)
        String referenceDataModelCa = referenceDataPluginMergeBuilder.buildCommonAncestorReferenceDataModel(commonAncestorId)

        [
            commonAncestorId     : commonAncestorId,
            dataModelCaId        : dataModelCa,
            terminologyCaId      : terminologyCa,
            codeSetCaId          : codeSetCa,
            referenceDataModelCa : referenceDataModelCa
        ]
    }

    Map buildComplexModelsForBranching(boolean finalise = true) {
        Map data = buildComplexModelsForFinalisation()

        // Finalise
        if (finalise) {
            PUT("versionedFolders/$data.commonAncestorId/finalise", [versionChangeType: 'Major'])
            verifyResponse OK, response
        }

        data
    }

    Map buildSubFolderModelsForBranching() {
        String simpleTerminologyId = getSimpleTerminologyId()

        loginEditor()

        POST('versionedFolders', [
            label: 'Functional Test VersionedFolder With Sub Folders'
        ])
        verifyResponse(CREATED, response)
        String commonAncestorId = responseBody().id

        POST("folders/${commonAncestorId}/folders", [
            label: 'Sub Folder in VersionedFolder'
        ])
        verifyResponse(CREATED, response)
        String subFolderId = responseBody().id
        POST("folders/${subFolderId}/folders", [
            label: 'Sub-Sub Folder in VersionedFolder'
        ])
        verifyResponse(CREATED, response)
        String subSubFolderId = responseBody().id
        POST("folders/${commonAncestorId}/folders", [
            label: 'Sub Folder 2 in VersionedFolder'
        ])
        verifyResponse(CREATED, response)
        String subFolder2Id = responseBody().id

        String dataModelCaId = dataModelPluginMergeBuilder.buildCommonAncestorDataModel(commonAncestorId, '1', simpleTerminologyId)
        String terminologyCa = terminologyPluginMergeBuilder.buildCommonAncestorTerminology(subFolderId)
        String codeSetCa = terminologyPluginMergeBuilder.buildCommonAncestorCodeSet(subSubFolderId, terminologyCa)
        String dataModel2Id = dataModelPluginMergeBuilder.buildCommonAncestorDataModel(subFolder2Id, '2', simpleTerminologyId)
        String dataModel3Id = dataModelPluginMergeBuilder.buildCommonAncestorDataModel(subSubFolderId, '3', simpleTerminologyId)
        dataModelPluginMergeBuilder.buildCommonAncestorModelDataType(dataModelCaId, terminologyCa)
        dataModelPluginMergeBuilder.buildCommonAncestorModelDataType(dataModel2Id, terminologyCa)
        dataModelPluginMergeBuilder.buildCommonAncestorModelDataType(dataModel3Id, terminologyCa)
        String referenceDataModelCaId = referenceDataPluginMergeBuilder.buildCommonAncestorReferenceDataModel(commonAncestorId)

        // Finalise
        PUT("versionedFolders/$commonAncestorId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        [
            commonAncestorId: commonAncestorId,
            dataModelCaId   : dataModelCaId,
            terminologyCaId : terminologyCa,
            codeSetCaId     : codeSetCa,
            dataModel2Id    : dataModel2Id,
            dataModel3Id    : dataModel3Id,
            referenceDataModelCaId:     referenceDataModelCaId
        ]
    }

    TestMergeData buildSimpleVersionedFoldersForMerging(boolean readLhs = true, boolean readRhs = true) {
        loginEditor()
        POST('versionedFolders', [
            label: 'Functional Test VersionedFolder Simple'
        ])
        verifyResponse(CREATED, response)
        String id = responseBody().id

        PUT("versionedFolders/$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("versionedFolders/$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id
        if (readRhs) addAccessShares(mainId)
        PUT("versionedFolders/$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        if (readLhs) addAccessShares(leftId)
        logout()
        new TestMergeData(commonAncestor: id, source: leftId, target: mainId)
    }

    TestMergeData buildSubFolderModelsForMerging() {
        String simpleTerminologyId = getSimpleTerminologyId()
        String complexTerminologyId = getComplexTerminologyId()

        loginEditor()

        Map data = buildSubFolderModelsForBranching()
        PUT("versionedFolders/$data.commonAncestorId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        addAccessShares(target)
        PUT("versionedFolders/$data.commonAncestorId/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id
        addAccessShares(source)

        Map<String, Object> sourceMap = [:]
        Map<String, Object> targetMap = [:]

        logout()
        loginEditor()

        sourceMap.dataModel1 = dataModelPluginMergeBuilder.modifySourceDataModel(source, '1', '', simpleTerminologyId, complexTerminologyId)
        targetMap.dataModel1 = dataModelPluginMergeBuilder.modifyTargetDataModel(target)

        sourceMap.dataModel2 = dataModelPluginMergeBuilder.modifySourceDataModel(source, '2', 'fo:Sub Folder 2 in VersionedFolder|', simpleTerminologyId, complexTerminologyId)
        targetMap.dataModel2 = dataModelPluginMergeBuilder.modifyTargetDataModel(target, '2', 'fo:Sub Folder 2 in VersionedFolder|')

        sourceMap.dataModel3 = dataModelPluginMergeBuilder.modifySourceDataModel(source, '3', 'fo:Sub Folder in VersionedFolder|fo:Sub-Sub Folder in VersionedFolder|', simpleTerminologyId, complexTerminologyId)
        targetMap.dataModel3 = dataModelPluginMergeBuilder.modifyTargetDataModel(target, '3', 'fo:Sub Folder in VersionedFolder|fo:Sub-Sub Folder in VersionedFolder|')

        sourceMap.terminology = terminologyPluginMergeBuilder.modifySourceTerminology(source, 'fo:Sub Folder in VersionedFolder|')
        targetMap.terminology = terminologyPluginMergeBuilder.modifyTargetTerminology(target, 'fo:Sub Folder in VersionedFolder|')

        sourceMap.codeSet = terminologyPluginMergeBuilder.modifySourceCodeSet(source, '$source', true,
                                                                              'fo:Sub Folder in VersionedFolder|fo:Sub-Sub Folder in VersionedFolder|',
                                                                              'fo:Sub Folder in VersionedFolder|')
        targetMap.codeSet = terminologyPluginMergeBuilder.modifyTargetCodeSet(target, '$main', true,
                                                                              'fo:Sub Folder in VersionedFolder|fo:Sub-Sub Folder in VersionedFolder|',
                                                                              'fo:Sub Folder in VersionedFolder|')

        sourceMap.referenceDataModel = referenceDataPluginMergeBuilder.modifySourceReferenceDataModel(source)
        targetMap.referenceDataModel = referenceDataPluginMergeBuilder.modifyTargetReferenceDataModel(target)

        sourceMap.subFolderId = getIdFromPath(source, 'fo:Sub Folder in VersionedFolder')
        sourceMap.subFolder2Id = getIdFromPath(source, 'fo:Sub Folder 2 in VersionedFolder')
        sourceMap.subSubFolderId = getIdFromPath(source, 'fo:Sub Folder in VersionedFolder|fo:Sub-Sub Folder in VersionedFolder')

        targetMap.subFolderId = getIdFromPath(target, 'fo:Sub Folder in VersionedFolder')
        targetMap.subFolder2Id = getIdFromPath(target, 'fo:Sub Folder 2 in VersionedFolder')
        targetMap.subSubFolderId = getIdFromPath(target, 'fo:Sub Folder in VersionedFolder|fo:Sub-Sub Folder in VersionedFolder')

        PUT("folders/${sourceMap.subFolderId}", [description: 'source description'])
        //        PUT("folders/${sourceMap.subSubFolderId}", [description: 'source sub sub description'])


        PUT("folders/${targetMap.subFolderId}", [description: 'target description'])
        //        PUT("folders/${targetMap.subSubFolderId}", [description: 'target sub sub description'])

        PUT("versionedFolders/$source", [description: 'source description on the versioned folder'])
        verifyResponse OK, response
        PUT("versionedFolders/$target", [description: 'Target modified description'])
        verifyResponse OK, response

        POST("folders/$source/dataModels", [label: 'Created DataModel in Source'])
        verifyResponse(CREATED, response)

        POST("folders/${source}/folders", [
            label: 'New Sub Folder in VersionedFolder'
        ])
        verifyResponse(CREATED, response)
        sourceMap.newSubFolderId = responseBody().id

        POST("folders/${sourceMap.newSubFolderId}/dataModels", [label: 'Created DataModel in sub folder'])
        verifyResponse(CREATED, response)

        POST("folders/${sourceMap.subFolder2Id}/folders", [
            label: 'New Sub-Sub Folder 2 in VersionedFolder'
        ])
        verifyResponse(CREATED, response)
        sourceMap.newSubSubFolder2Id = responseBody().id

        POST("folders/${sourceMap.newSubSubFolder2Id}/dataModels", [label: 'Created DataModel in sub sub folder'])
        verifyResponse(CREATED, response)

        //       sourceMap.dataModel4 = dataModelPluginMergeBuilder.buildCommonAncestorDataModel(sourceMap.newSubSubFolder2Id.toString(),'4')

        // Point the Model Data Type in the source to point at the Code Set rather than Terminology
        PUT("dataModels/$sourceMap.dataModel1.dataModelId/dataTypes/$sourceMap.dataModel1.modelDataTypeId", [
            modelResourceDomainType: 'CodeSet', modelResourceId: sourceMap.codeSet.codeSetId
        ])
        verifyResponse OK, response
        logout()


        new TestMergeData(commonAncestor: data.commonAncestorId,
                          source: source,
                          target: target,
                          sourceMap: sourceMap,
                          targetMap: targetMap
        )
    }

    TestMergeData buildComplexModelsForMerging() {
        buildComplexModelsForMerging(null)
    }

    @Override
    TestMergeData buildComplexModelsForMerging(String folderId) {
        String simpleTerminologyId = getSimpleTerminologyId()
        String complexTerminologyId = getComplexTerminologyId()

        loginEditor()

        Map data = buildComplexModelsForBranching()
        PUT("versionedFolders/$data.commonAncestorId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        addAccessShares(target)
        PUT("versionedFolders/$data.commonAncestorId/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id
        addAccessShares(source)

        Map<String, Object> sourceMap = [:]
        Map<String, Object> targetMap = [:]

        logout()
        loginEditor()

        sourceMap.dataModel = dataModelPluginMergeBuilder.modifySourceDataModel(source, '1', '', simpleTerminologyId, complexTerminologyId)
        targetMap.dataModel = dataModelPluginMergeBuilder.modifyTargetDataModel(target)

        sourceMap.referenceDataModel = referenceDataPluginMergeBuilder.modifySourceReferenceDataModel(source)
        targetMap.referenceDataModel = referenceDataPluginMergeBuilder.modifyTargetReferenceDataModel(target)

        sourceMap.terminology = terminologyPluginMergeBuilder.modifySourceTerminology(source)
        targetMap.terminology = terminologyPluginMergeBuilder.modifyTargetTerminology(target)

        sourceMap.codeSet = terminologyPluginMergeBuilder.modifySourceCodeSet(source, '$source', true)
        targetMap.codeSet = terminologyPluginMergeBuilder.modifyTargetCodeSet(target, '$main', true)

        PUT("versionedFolders/$source", [description: 'source description on the versioned folder'])
        verifyResponse OK, response
        PUT("versionedFolders/$target", [description: 'Target modified description'])
        verifyResponse OK, response

        // Point the Model Data Type in the source to point at the Code Set rather than Terminology
        PUT("dataModels/$sourceMap.dataModel.dataModelId/dataTypes/$sourceMap.dataModel.modelDataTypeId", [
            modelResourceDomainType: 'CodeSet', modelResourceId: sourceMap.codeSet.codeSetId
        ])
        verifyResponse OK, response

        POST("folders/$source/dataModels", [label: 'Created DataModel in Source'])
        verifyResponse(CREATED, response)


        //        POST("$source/metadata", [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'])
        //        verifyResponse CREATED, response
        //        PUT("$source/metadata/$sourceMap.metadataModifyOnSource", [value: 'source has modified this'])
        //        verifyResponse OK, response
        //        PUT("$source/metadata/$sourceMap.metadataModifyAndDelete", [value: 'source has modified this also'])
        //        verifyResponse OK, response
        //        DELETE("$source/metadata/$sourceMap.metadataDeleteFromSource")
        //        verifyResponse NO_CONTENT, response
        //        DELETE("$target/metadata/$targetMap.metadataModifyAndDelete")
        //        verifyResponse NO_CONTENT, response
        logout()


        new TestMergeData(commonAncestor: data.commonAncestorId,
                          source: source,
                          target: target,
                          sourceMap: sourceMap,
                          targetMap: targetMap
        )
    }

    HttpResponse<Map> loginEditor() {
        fullFunctionalSpec.loginEditor()
    }

    void addAccessShares(String id) {
        fullFunctionalSpec.addAccessShares(id)
    }

    void logout() {
        fullFunctionalSpec.logout()
    }

    void removeValidIdObject(String id) {
        fullFunctionalSpec.removeValidIdObject(id)
    }

    @Override
    void cleanupTestMergeData(TestMergeData mergeData) {
        if (!mergeData) return
        if (mergeData.source) removeValidIdObject(mergeData.source)
        if (mergeData.target) removeValidIdObject(mergeData.target)
        if (mergeData.commonAncestor) removeValidIdObject(mergeData.commonAncestor)
    }

    static String getExpectedMergeDiffJson() {
        Files.readString(Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'versionedFolders', 'MergeDiff.json'))
    }

    static String getExpectedSubFolderMergeDiffJson() {
        Files.readString(Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'versionedFolders', 'SubFolderMergeDiff.json'))
    }

    String getSimpleTerminologyId() {
        loginEditor()
        GET('terminologies/path/te:Simple%20Test%20Terminology')
        verifyResponse(OK, response)
        String simpleTerminologyId = responseBody().id
        logout()
        simpleTerminologyId
    }

    String getComplexTerminologyId() {
        loginEditor()
        GET('terminologies/path/te:Complex%20Test%20Terminology')
        verifyResponse(OK, response)
        String complexTerminologyId = responseBody().id
        logout()
        complexTerminologyId
    }
}
