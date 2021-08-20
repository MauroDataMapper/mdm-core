package uk.ac.ox.softeng.maurodatamapper.testing.functional.merge

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.BaseTestMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.DataModelPluginMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.TerminologyPluginMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.TestMergeData
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

class VersionedFolderMergeBuilder extends BaseTestMergeBuilder {

    DataModelPluginMergeBuilder dataModelPluginMergeBuilder
    TerminologyPluginMergeBuilder terminologyPluginMergeBuilder

    UserAccessAndPermissionChangingFunctionalSpec fullFunctionalSpec

    VersionedFolderMergeBuilder(UserAccessAndPermissionChangingFunctionalSpec functionalSpec) {
        super(functionalSpec)
        this.fullFunctionalSpec = functionalSpec
        dataModelPluginMergeBuilder = new DataModelPluginMergeBuilder(functionalSpec)
        terminologyPluginMergeBuilder = new TerminologyPluginMergeBuilder(functionalSpec)
    }

    Map buildComplexModelsForBranching() {
        // Somethings up with the MD, when running properly the diff happily returns the changed MD, but under test it doesnt.
        // The MD exists in the daabase and is returned if using the MD endpoint but when calling folder.metadata the collection is empty.
        // When run-app all the tables are correctly populated and the collection is not empty
        loginEditor()
        POST("versionedFolders", [
            label: 'Functional Test VersionedFolder Complex'
        ])
        verifyResponse(CREATED, response)
        String commonAncestorId = responseBody().id
        //        POST("$commonAncestorId/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'])
        //        verifyResponse CREATED, response
        //        POST("$commonAncestorId/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'])
        //        verifyResponse CREATED, response
        //        POST("$commonAncestorId/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'])
        //        verifyResponse CREATED, response
        //        POST("$commonAncestorId/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'])
        //        verifyResponse CREATED, response3

        String dataModelCa = dataModelPluginMergeBuilder.buildCommonAncestorDataModel(commonAncestorId)
        String terminologyCa = terminologyPluginMergeBuilder.buildCommonAncestorTerminology(commonAncestorId)
        String codeSetCa = terminologyPluginMergeBuilder.buildCommonAncestorCodeSet(commonAncestorId, terminologyCa)

        // Finalise and branch
        PUT("versionedFolders/$commonAncestorId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        [
            commonAncestorId: commonAncestorId,
            dataModelCaId   : dataModelCa,
            terminologyCaId : terminologyCa,
            codeSetCaId     : codeSetCa
        ]
    }

    TestMergeData buildSimpleVersionedFoldersForMerging(boolean readLhs = true, boolean readRhs = true) {
        loginEditor()
        POST("versionedFolders", [
            label: 'Functional Test VersionedFolder Simple'
        ])
        verifyResponse(CREATED, response)
        String id = responseBody().id

        PUT("versionedFolders/$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("versionedFolders/$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id
        if (readRhs) addReaderShare(mainId)
        PUT("versionedFolders/$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        if (readLhs) addReaderShare(leftId)
        logout()
        new TestMergeData(commonAncestor: id, source: leftId, target: mainId)
    }

    TestMergeData buildComplexModelsForMerging() {
        buildComplexModelsForMerging(null)
    }

    @Override
    TestMergeData buildComplexModelsForMerging(String folderId) {
        loginEditor()

        Map data = buildComplexModelsForBranching()
        PUT("versionedFolders/$data.commonAncestorId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        addReaderShare(target)
        PUT("versionedFolders/$data.commonAncestorId/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id
        addReaderShare(source)

        Map<String, Object> sourceMap = [:]
        Map<String, Object> targetMap = [:]

        logout()
        loginEditor()

        sourceMap.dataModel = dataModelPluginMergeBuilder.modifySourceDataModel(source)
        targetMap.dataModel = dataModelPluginMergeBuilder.modifyTargetDataModel(target)

        sourceMap.terminology = terminologyPluginMergeBuilder.modifySourceTerminology(source)
        targetMap.terminology = terminologyPluginMergeBuilder.modifyTargetTerminology(target)

        sourceMap.codeSet = terminologyPluginMergeBuilder.modifySourceCodeSet(source, '$source', true)
        targetMap.codeSet = terminologyPluginMergeBuilder.modifyTargetCodeSet(target, '$main', true)

        PUT("versionedFolders/$source", [description: 'source description on the versioned folder'])
        verifyResponse OK, response
        PUT("versionedFolders/$target", [description: 'Target modified description'])
        verifyResponse OK, response


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

    void addReaderShare(String id) {
        fullFunctionalSpec.addReaderShare(id)
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
        '''{
  "sourceId": "${json-unit.matches:id}",
  "targetId": "${json-unit.matches:id}",
  "path": "vf:Functional Test VersionedFolder Complex$source",
  "label": "Functional Test VersionedFolder Complex",
  "count": 44,
  "diffs": [
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source@description",
      "sourceValue": "source description on the versioned folder",
      "targetValue": "Target modified description",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|cs:Functional Test CodeSet 1$source@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|cs:Functional Test CodeSet 1$source|md:functional.test.addToSourceOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|cs:Functional Test CodeSet 1$source|md:functional.test.modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|cs:Functional Test CodeSet 1$source|md:functional.test.deleteFromSource",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "value",
      "path": "vf:Functional Test VersionedFolder Complex$source|cs:Functional Test CodeSet 1$source|md:functional.test.modifyOnSource@value",
      "sourceValue": "source has modified this",
      "targetValue": "some original value",
      "commonAncestorValue": "some original value",
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|cs:Functional Test CodeSet 1$source|te:Functional Test Terminology 1$source|tm:ALO",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:addLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:deleteAndModify",
      "isMergeConflict": true,
      "isSourceDeletionAndTargetModification": true,
      "type": "deletion"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:deleteLeftOnly",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:addAndAddReturningDifference@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:existingClass|dc:addLeftToExistingClass",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:existingClass|dc:deleteLeftOnlyFromExistingClass",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:modifyAndModifyReturningDifference@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|dc:modifyLeftOnly@description",
      "sourceValue": "Description",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|md:functional.test.addToSourceOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|md:functional.test.modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|md:functional.test.deleteFromSource",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "value",
      "path": "vf:Functional Test VersionedFolder Complex$source|dm:Functional Test DataModel 1$source|md:functional.test.modifyOnSource@value",
      "sourceValue": "source has modified this",
      "targetValue": "some original value",
      "commonAncestorValue": "some original value",
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|md:functional.test.addToSourceOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|md:functional.test.modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|md:functional.test.deleteFromSource",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "value",
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|md:functional.test.modifyOnSource@value",
      "sourceValue": "source has modified this",
      "targetValue": "some original value",
      "commonAncestorValue": "some original value",
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|trt:sameActionAs",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|trt:oppositeActionTo",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|trt:inverseOf@description",
      "sourceValue": "inverseOf(Modified)",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tr:ALO.sameSourceActionType.SALO",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tr:ALO.similarSourceAction.AAARD",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tr:MLO.similarSourceAction.MAMRD",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tr:SMLO.sameSourceActionType.MLO@description",
      "sourceValue": "NewDescription",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tm:ALO",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tm:MAD",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tm:SALO",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tm:DAM",
      "isMergeConflict": true,
      "isSourceDeletionAndTargetModification": true,
      "type": "deletion"
    },
    {
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tm:DLO",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tm:AAARD@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tm:MAMRD@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder Complex$source|te:Functional Test Terminology 1$source|tm:MLO@description",
      "sourceValue": "Description",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    }
  ]
}'''
    }
}
